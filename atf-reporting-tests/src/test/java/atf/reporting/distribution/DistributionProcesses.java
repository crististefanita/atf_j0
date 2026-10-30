package atf.reporting.distribution;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Bounded test-only process boundary; every working directory and output belongs to the private probe. */
final class DistributionProcesses {
    private DistributionProcesses() { }
    static String maven(Path directory,String name,String... arguments) throws Exception {
        var command=new ArrayList<String>();
        if(System.getProperty("os.name").startsWith("Windows")) command.addAll(List.of("cmd.exe","/d","/c","mvn"));
        else command.add("mvn");
        command.addAll(List.of("-B","-ntp"));command.addAll(List.of(arguments));
        var result=run(directory,name,command);
        if(result.exit()!=0) throw new IllegalStateException("Maven probe failed: "+result.log()+"\n"+result.text());
        return result.text();
    }
    static Result run(Path directory,String name,List<String> command) throws Exception {
        Path log=directory.resolve(name+".log");
        var process=new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(180,TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);process.destroyForcibly();
            throw new IllegalStateException("Distribution probe exceeded its bounded process lifetime: "+log);
        }
        return new Result(process.exitValue(),log,Files.readString(log));
    }
    record Result(int exit,Path log,String text) { }
}
