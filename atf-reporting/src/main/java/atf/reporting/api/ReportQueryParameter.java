package atf.reporting.api;
public record ReportQueryParameter(String name, String value) {
    public ReportQueryParameter { if(name==null||name.isBlank()||value==null) throw new IllegalArgumentException("query parameter must be named and non-null"); }
}
