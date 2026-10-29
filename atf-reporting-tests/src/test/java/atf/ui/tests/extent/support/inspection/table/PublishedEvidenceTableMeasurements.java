package atf.ui.tests.extent.support.inspection.table;

import atf.ui.tests.extent.support.locators.table.PublishedEvidenceTableLocators;
import com.microsoft.playwright.*;
import java.util.*;

/** Browser batches expose physical measurements, independently from renderer layout markers. */
final class PublishedEvidenceTableMeasurements {
    private static final PublishedEvidenceTableLocators LOCATORS = new PublishedEvidenceTableLocators();
    private static final String DATA = """
            t => {
              const headers=Array.from(t.tHead?.rows[0]?.cells||[],h=>h.textContent.trim().toUpperCase());
              const known=Array.from(t.classList).some(c=>/assertions|helm-|atf-ui-|atf-setup-/.test(c))
                ||Array.from(t.parentElement.classList).some(c=>/atf-.*table-(wrap|viewport)/.test(c));
              return known || headers.length>1 && !(headers.includes('DETAILS') &&
                headers.every(h=>['STATUS','TIMESTAMP','DETAILS'].includes(h)));
            }
            """;

    private PublishedEvidenceTableMeasurements() { }

    static List<Locator> dataTables(Page page) {
        var tables = LOCATORS.tables(page);
        List<?> indexes = (List<?>) tables.evaluateAll("tables=>tables.flatMap((t,i)=>(" + DATA + ")(t)?[i]:[])");
        return indexes.stream().map(i -> tables.nth(((Number) i).intValue())).toList();
    }

    @SuppressWarnings("unchecked")
    static List<String> contents(Page page) {
        return (List<String>) LOCATORS.tables(page).evaluateAll(
                "tables=>tables.filter(" + DATA + ").filter(t=>{for(let p=t.parentElement;p;p=p.parentElement)"
                        + "if(p.classList.contains('atf-ui-failure-evidence'))return false;return true;})"
                        + ".map(t=>t.textContent.replace(/\\s+/g,' ').trim())");
    }

    static List<Table> snapshot(Page page) {
        List<?> rows = (List<?>) LOCATORS.tables(page).evaluateAll("tables=>tables.filter(" + DATA + ").map(t=>{"
                + "const owners=[],disclosure=[];let found=false;"
                + "for(let p=t.parentElement;p;p=p.parentElement){owners.push(...p.classList);"
                + "if(!found&&p.tagName==='DETAILS'){disclosure.push(...p.classList);found=true;}}"
                + "return {family:t.className,owners,disclosure,"
                + "headers:Array.from(t.tHead?.rows[0]?.cells||[],c=>c.textContent.trim().toUpperCase()),"
                + "rows:Array.from(t.tBodies).flatMap(b=>Array.from(b.rows,r=>Array.from(r.cells,c=>c.innerText.trim())))};})");
        return rows.stream().map(row -> Table.from((Map<?, ?>) row)).toList();
    }

    static Map<?, ?> geometry(Page page) {
        return (Map<?, ?>) LOCATORS.tables(page).evaluateAll("tables=>{const rows=tables.filter(" + DATA
                + ").map(t=>({" + "family:t.className,problems:(" + GEOMETRY + ")(t)}));"
                + """
                const width=document.documentElement.clientWidth,overflow=document.documentElement.scrollWidth>width+2;
                const offenders=overflow?Array.from(document.body.getElementsByTagName('*')).map(e=>{
                  const r=e.getBoundingClientRect(),s=getComputedStyle(e);
                  return {tag:e.tagName,classes:e.className?.toString().slice(0,160),right:Math.round(r.right),
                    left:Math.round(r.left),width:Math.round(r.width),height:r.height,display:s.display,
                    overflowX:s.overflowX,position:s.position,minWidth:s.minWidth,grid:s.gridTemplateColumns,
                    parent:e.parentElement?.className?.toString().slice(0,120)};
                }).filter(e=>e.height>0&&e.display!=='none'&&e.right>width+2)
                  .sort((a,b)=>b.right-a.right).slice(0,12):[];
                return {tables:rows,overflow,width,offenders};}
                """);
    }

    private static final String GEOMETRY = """
            t => {
              const problems=[],p=t.parentElement,s=getComputedStyle(p),box=p.getBoundingClientRect();
              const b=parseFloat(s.borderLeftWidth)+parseFloat(s.borderRightWidth);
              const scrollbar=p.scrollHeight>p.clientHeight?p.offsetWidth-p.clientWidth-b:0;
              const available=box.width-b-parseFloat(s.paddingLeft)-parseFloat(s.paddingRight)-scrollbar;
              const minimum=parseFloat(getComputedStyle(t).minWidth)||0;
              if(Math.abs(t.getBoundingClientRect().width-Math.max(available,minimum))>2)problems.push('unused width');
              if(s.overflowX!=='auto'||s.overflowY!=='auto')problems.push('scroll policy');
              const cap=parseFloat(s.maxHeight),token=parseFloat(getComputedStyle(document.documentElement)
                .getPropertyValue('--atf-table-scroll-height'));
              if(!Number.isFinite(cap)||!Number.isFinite(token)||Math.abs(cap-token)>2
                  ||p.clientHeight>cap+2)problems.push('vertical cap');
              const heads=Array.from(t.tHead?.rows[0]?.cells||[]);
              if(!heads.length)return ['missing header'];
              if(getComputedStyle(t).display!=='table'||t.tHead.getBoundingClientRect().width<2
                  ||getComputedStyle(t.tHead).clipPath!=='none')problems.push('hidden header');
              heads.forEach(h=>{
                if(h.scrollWidth>h.clientWidth+2||h.scrollHeight>h.clientHeight+2)problems.push('header clipped');
                if(['STATUS','TIMESTAMP'].includes(h.textContent.trim().toUpperCase())){
                  const range=document.createRange();range.selectNodeContents(h);
                  const tops=Array.from(range.getClientRects()).filter(r=>r.width>0).map(r=>r.top);
                  if(tops.length&&Math.max(...tops)-Math.min(...tops)>2)problems.push('technical header wraps');
                }
              });
              Array.from(t.tBodies).forEach(body=>Array.from(body.rows).forEach(row=>{
                const cells=Array.from(row.cells);
                if(cells.length!==heads.length)problems.push('missing column');
                cells.forEach((c,i)=>{
                  const a=c.getBoundingClientRect(),h=heads[i]?.getBoundingClientRect();
                  if(!h||a.height<1||getComputedStyle(c).display!=='table-cell'
                      ||Math.abs(a.x-h.x)>2||Math.abs(a.width-h.width)>2
                      ||Math.abs(a.y-cells[0].getBoundingClientRect().y)>2)problems.push('cell outside column');
                  if(parseFloat(getComputedStyle(c).fontSize)<12)problems.push('font compressed');
                  if(c.scrollWidth>c.clientWidth+2||c.scrollHeight>c.clientHeight+2)problems.push('text clipped');
                  Array.from(c.getElementsByTagName('*')).forEach(child=>{
                    const style=getComputedStyle(child);
                    if((/hidden|clip/.test(style.overflowX)&&child.scrollWidth>child.clientWidth+2)
                        ||(/hidden|clip/.test(style.overflowY)&&child.scrollHeight>child.clientHeight+2))
                      problems.push('nested text clipped');
                  });
                });
              }));
              p.scrollLeft=p.scrollWidth;
              const lastColumn=t.tHead.rows[0].lastElementChild;
              if(lastColumn.getBoundingClientRect().right>box.right+2)problems.push('last column unreachable');
              p.scrollLeft=0;
              p.scrollTop=p.scrollHeight;
              const last=t.tBodies[t.tBodies.length-1]?.lastElementChild;
              if(last&&last.getBoundingClientRect().bottom>box.bottom+2)problems.push('last row unreachable');
              if(p.scrollHeight>p.clientHeight+2&&heads.some(h=>getComputedStyle(h).position!=='sticky'
                  ||Math.abs(h.getBoundingClientRect().top-box.top)>3))problems.push('header not sticky');
              p.scrollTop=0;
              return [...new Set(problems)];
            }
            """;

    record Table(String family, List<String> headers, List<List<String>> rows,
                 List<String> owners, List<String> disclosure) {
        @SuppressWarnings("unchecked")
        static Table from(Map<?, ?> value) {
            return new Table((String) value.get("family"), (List<String>) value.get("headers"),
                    (List<List<String>>) value.get("rows"), (List<String>) value.get("owners"),
                    (List<String>) value.get("disclosure"));
        }

        boolean has(String token) { return Arrays.asList(family.split("\\s+")).contains(token); }
        boolean owns(String token) { return owners.contains(token); }
        boolean inDisclosure(String token) { return disclosure.contains(token); }
        boolean assertions() {
            return has("helm-validation-table") || has("assertions")
                    || !headers.isEmpty() && headers.getFirst().equals("ASSERTION");
        }
    }
}

