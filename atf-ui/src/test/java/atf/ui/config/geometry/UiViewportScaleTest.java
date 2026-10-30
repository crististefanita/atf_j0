package atf.ui.config.geometry;

import atf.testkit.AtfUnitTest;
import atf.ui.config.UiBrowserConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UiViewportScaleTest {
    @ParameterizedTest @CsvSource({"25,5760,4000,0.25","70,2057,1429,0.70","100,1440,1000,1.00","125,1152,800,1.25","200,720,500,2.00"})
    void configuredOutputAndNativeLayoutHaveAnExplicitPlan(int zoom,int width,int height,double factor){
        var config=configuration(1440,1000,zoom);var geometry=new UiViewportScale(config);
        assertEquals(width,geometry.layoutWidth());assertEquals(height,geometry.layoutHeight());
        assertEquals(factor,geometry.deviceScaleFactor());
        assertEquals(1440,config.viewportWidth());assertEquals(1000,config.viewportHeight());
        assertTrue(geometry.description().contains("1440 × 1000 requested output"));
    }
    @Test void anUnrepresentableLayoutIsRefusedBeforeBrowserAcquisition(){
        assertThrows(IllegalArgumentException.class,()->new UiViewportScale(configuration(Integer.MAX_VALUE,1000,25)));
    }
    private static UiBrowserConfiguration configuration(int width,int height,int zoom){
        return new UiBrowserConfiguration(URI.create("about:blank"),"chromium",true,Duration.ofSeconds(1),Duration.ofSeconds(5),width,height,zoom,true);
    }
}
