package atf.reporting.distribution;

import atf.reporting.extent.session.ExtentReportSession;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Consumes the actual deployment: isolated producer build or physical JAR with embedded Maven metadata. */
final class ReportingDistributionProbe {
    private ReportingDistributionProbe() { }
    static Result verify(Path directory) throws Exception {
        var input=prepareArtifact(directory);
        var coordinates=input.coordinates();
        Path artifact=input.jar();
        String hash=hash(artifact);
        Path repository=directory.resolve("published").resolve(coordinates.group().replace('.','/'))
                .resolve(coordinates.artifact()).resolve(coordinates.version());Files.createDirectories(repository);
        Files.copy(artifact,repository.resolve(artifact.getFileName()));
        Files.copy(input.pom(),repository.resolve(coordinates.fileName()+".pom"));
        Path consumer=directory.resolve("external-consumer");Files.createDirectories(consumer.resolve("src/main/java"));
        try(var source=ReportingDistributionProbe.class.getResourceAsStream("/atf/reporting/distribution/JarConsumer.java.txt")) {
            if(source==null) throw new IllegalStateException("Standalone consumer fixture is missing");
            Files.copy(source,consumer.resolve("src/main/java/JarConsumer.java"));
        }
        Files.writeString(consumer.resolve("pom.xml"),pom(directory.resolve("published"),coordinates));
        DistributionProcesses.maven(consumer,"external-compile","-Dmaven.repo.local="+directory.resolve("consumer-cache"),
                "compile","org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath",
                "-DincludeScope=runtime","-Dmdep.outputFile="+consumer.resolve("runtime.cp"));
        String dependencies=Files.readString(consumer.resolve("runtime.cp")).trim();
        String classpath=consumer.resolve("target/classes")+java.io.File.pathSeparator+dependencies;
        var nominal=DistributionProcesses.run(consumer,"nominal",java(classpath,consumer.resolve("reports"),hash,false));
        Path broken=directory.resolve("missing-resource.jar");removeAsset(artifact,broken);
        String brokenDependencies=Arrays.stream(dependencies.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
                .map(value->Path.of(value).getFileName().toString().equals(artifact.getFileName().toString())?broken.toString():value)
                .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        String brokenClasspath=consumer.resolve("target/classes")+java.io.File.pathSeparator+brokenDependencies;
        var extentRefusal=DistributionProcesses.run(consumer,"missing-extent-resource",java(brokenClasspath,consumer.resolve("negative-extent"),hash(broken),false));
        var fallbackRefusal=DistributionProcesses.run(consumer,"missing-fallback-resource",java(brokenClasspath,consumer.resolve("negative-fallback"),hash(broken),true));
        return new Result(input.build(),nominal,extentRefusal,fallbackRefusal,hash,input.sourceBuild());
    }
    private static Artifact prepareArtifact(Path directory) throws Exception {
        Path loaded=Path.of(ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if(Files.isRegularFile(loaded)) {
            Path pom=directory.resolve("binary-pom.xml");
            try(var jar=new java.util.jar.JarFile(loaded.toFile())) {
                var metadata=jar.getJarEntry("META-INF/maven/atf.api/atf-reporting/pom.xml");
                if(metadata==null)throw new IllegalStateException("Reporting JAR has no embedded Maven POM");
                try(var stream=jar.getInputStream(metadata)){Files.copy(stream,pom);}
            }
            var coordinates=coordinates(pom);
            Path artifact=directory.resolve(coordinates.fileName()+".jar");
            Files.copy(loaded,artifact);
            return new Artifact(artifact,pom,coordinates,"",false);
        }
        Path module=moduleDirectory();
        var coordinates=coordinates(module.resolve("pom.xml"));
        Path extracted=directory.resolve("isolated-source");Files.createDirectories(extracted);
        Files.copy(module.resolve("pom.xml"),extracted.resolve("pom.xml"));
        copyTree(module.resolve("src"),extracted.resolve("src"));
        String build=DistributionProcesses.maven(extracted,"isolated-package","package");
        return new Artifact(extracted.resolve("target/"+coordinates.fileName()+".jar"),
                extracted.resolve("pom.xml"),coordinates,build,true);
    }
    private record Artifact(Path jar,Path pom,Coordinates coordinates,String build,boolean sourceBuild) { }
    private static List<String> java(String classpath,Path reports,String hash,boolean fallback) {
        String executable=System.getProperty("os.name").startsWith("Windows")?"java.exe":"java";
        var command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin",executable).toString(),
                "-Xmx384m","-cp",classpath,"JarConsumer",reports.toString(),hash));
        if(fallback) command.add("FALLBACK");return command;
    }
    private static String pom(Path repository,Coordinates coordinates) {
        String cache=Path.of(System.getProperty("user.home"),".m2","repository").toUri().toString();
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion><groupId>probe</groupId><artifactId>reporting-consumer</artifactId><version>1</version>
                  <properties><maven.compiler.release>21</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
                  <repositories><repository><id>private-artifact</id><url>%s</url></repository>
                    <repository><id>cached-vendors</id><url>%s</url></repository></repositories>
                  <pluginRepositories><pluginRepository><id>cached-plugins</id><url>%s</url></pluginRepository></pluginRepositories>
                  <dependencies><dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version></dependency></dependencies>
                  <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.13.0</version><configuration><proc>none</proc></configuration></plugin></plugins></build>
                </project>
                """.formatted(repository.toUri(),cache,cache,coordinates.group(),coordinates.artifact(),coordinates.version());
    }
    private static Path moduleDirectory() throws Exception {
        Path current=Path.of(ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        while(current!=null) {
            if(Files.isRegularFile(current.resolve("pom.xml"))&&Files.isDirectory(current.resolve("src/main/java/atf/reporting"))) return current;
            current=current.getParent();
        }
        throw new IllegalStateException("Reporting source module cannot be located from the active reactor classes");
    }
    private static void copyTree(Path source,Path target) throws Exception {
        try(var paths=Files.walk(source)) {
            for(Path file:paths.toList()) {
                Path destination=target.resolve(source.relativize(file));
                if(Files.isDirectory(file)) Files.createDirectories(destination);else Files.copy(file,destination);
            }
        }
    }
    private static void removeAsset(Path source,Path target) throws Exception {
        boolean removed=false;
        try(var input=new ZipInputStream(Files.newInputStream(source));var output=new ZipOutputStream(Files.newOutputStream(target))) {
            for(var entry=input.getNextEntry();entry!=null;entry=input.getNextEntry()) {
                if(entry.getName().equals("atf/reporting/css/table-viewport.css")) { removed=true;continue; }
                output.putNextEntry(new ZipEntry(entry.getName()));input.transferTo(output);output.closeEntry();
            }
        }
        if(!removed) throw new IllegalStateException("Negative control did not remove an existing asset");
    }
    private static String hash(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
    private static Coordinates coordinates(Path pom) throws Exception {
        var factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD,"");
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        var document=factory.newDocumentBuilder().parse(pom.toFile());
        var values=new HashMap<String,String>();
        for(var child=document.getDocumentElement().getFirstChild();child!=null;child=child.getNextSibling())
            if(child instanceof org.w3c.dom.Element element) values.put(element.getLocalName(),element.getTextContent().trim());
        return new Coordinates(values.get("groupId"),values.get("artifactId"),values.get("version"));
    }
    private record Coordinates(String group,String artifact,String version) {
        Coordinates {
            for(String value:List.of(group,artifact,version))
                if(value.isBlank()||value.contains("${")) throw new IllegalArgumentException("The standalone module needs explicit Maven coordinates");
        }
        String fileName() { return artifact+"-"+version; }
    }
    record Result(String isolatedBuild,DistributionProcesses.Result nominal,DistributionProcesses.Result extentRefusal,
                  DistributionProcesses.Result fallbackRefusal,String jarHash,boolean sourceBuild) { }
}
