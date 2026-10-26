package atf.reporting.console;

import atf.reporting.contract.AtfReportStatus;

import java.nio.file.Path;

public record AtfConsoleEvent(Type type, Scope scope, String name, AtfReportStatus status,
                              long durationMillis, String detailName, String detailValue) {
    public enum Type { START, DETAIL, CODE, COMPLETE, REPORT_PUBLISHED }
    public enum Scope { SCENARIO, PHASE, REQUEST, VALIDATION, UI_ACTION, REPORT }
    public AtfConsoleEvent {
        if (type == null || scope == null) throw new NullPointerException("type/scope");
        name=name==null?"":name.trim(); detailName=detailName==null?"":detailName.trim(); detailValue=detailValue==null?"":detailValue;
        if(durationMillis<0) throw new IllegalArgumentException("durationMillis must not be negative");
        if(type==Type.COMPLETE && status==null) throw new NullPointerException("status");
        if(type==Type.REPORT_PUBLISHED
                && (scope != Scope.REPORT || !Path.of(detailValue).isAbsolute())) {
            throw new IllegalArgumentException("published report event requires an absolute report path");
        }
    }
    public static AtfConsoleEvent started(Scope scope,String name){return new AtfConsoleEvent(Type.START,scope,name,null,0,"","");}
    public static AtfConsoleEvent detail(Scope scope,String name,String value){return new AtfConsoleEvent(Type.DETAIL,scope,"",null,0,name,value);}
    public static AtfConsoleEvent code(Scope scope,String name,String value){return new AtfConsoleEvent(Type.CODE,scope,"",null,0,name,value);}
    public static AtfConsoleEvent completed(Scope scope,String name,AtfReportStatus status,long duration){return new AtfConsoleEvent(Type.COMPLETE,scope,name,status,duration,"","");}
    public static AtfConsoleEvent reportPublished(Path reportPath) {
        Path normalized = java.util.Objects.requireNonNull(reportPath, "reportPath").toAbsolutePath().normalize();
        return new AtfConsoleEvent(Type.REPORT_PUBLISHED, Scope.REPORT, "Extent report",
                null, 0, "path", normalized.toString());
    }
}
