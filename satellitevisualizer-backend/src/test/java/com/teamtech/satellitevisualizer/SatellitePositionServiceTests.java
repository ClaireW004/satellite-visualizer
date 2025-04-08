package com.teamtech.satellitevisualizer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.teamtech.satellitevisualizer.models.SatelliteData;
import com.teamtech.satellitevisualizer.repository.SatelliteRepository;
import com.teamtech.satellitevisualizer.service.SatellitePositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

@ExtendWith(MockitoExtension.class)
class SatellitePositionServiceTests {

    @Mock
    private SatelliteRepository satelliteRepository;

    @InjectMocks
    private SatellitePositionService satellitePositionService;

    private final String validTLE =
            // database test:
            "1 25544U 98067A   25048.86150170  .00016610  00000-0  29603-3 0  9994\n" +
                    "2 25544  51.6391 178.0267 0004105 330.1473 172.8419 15.50222990496656";

    // replace with tle from n2yo for live testing:
    //"1 25544U 98067A   25077.86855735  .00037299  00000-0  66721-3 0  9993\n"+
    //"2 25544  51.6402  34.3664 0004296  19.8911  61.0593 15.49618050501151";

    @BeforeEach
    void setUp() {
        // loads orekit data
        File orekitData = new File("src/main/resources/orekit-data");
        DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));

        MockitoAnnotations.openMocks(this);
    }


    // ensures that TLE data is parsed into two lines
    @Test
    void testParseTLE() {
        Optional<TLE> tleOptional = SatellitePositionService.parseTLE(validTLE);

        assertTrue(tleOptional.isPresent(), "tle should be parsed");

        TLE tle = tleOptional.get();
        assertEquals("1 25544U 98067A   25048.86150170  .00016610  00000-0  29603-3 0  9994", tle.getLine1());
        assertEquals("2 25544  51.6391 178.0267 0004105 330.1473 172.8419 15.50222990496656", tle.getLine2());
    }

    @Test
    void testFetchTLE() {
        SatelliteData mockSatelliteData = mock(SatelliteData.class);
        when(mockSatelliteData.getTle()).thenReturn(validTLE);
        when(satelliteRepository.findById("25544")).thenReturn(Optional.of(mockSatelliteData));

        Optional<TLE> tleOptional = satellitePositionService.fetchTLE(25544);

        assertTrue(tleOptional.isPresent(), "tle should be retrieved");
        assertEquals("1 25544U 98067A   25048.86150170  .00016610  00000-0  29603-3 0  9994", tleOptional.get().getLine1());
        assertEquals("2 25544  51.6391 178.0267 0004105 330.1473 172.8419 15.50222990496656", tleOptional.get().getLine2());
    }

    // fetches the current LLA for iss 25544
    @Test
    void testGetCurrentLLA() {
        SatelliteData mockSatelliteData = mock(SatelliteData.class);
        when(mockSatelliteData.getTle()).thenReturn(validTLE);
        when(satelliteRepository.findById("25544")).thenReturn(Optional.of(mockSatelliteData));
        System.out.println("--- CurrentLLA Orekit Output ---");
        satellitePositionService.getCurrentLLA(25544);
    }

    // fetches the future LLA for iss 25544
//    // todo: i dont think this is accurate
//    @Test
//    void testGetFutureLLA() {
//        SatelliteData mockSatelliteData = mock(SatelliteData.class);
//        when(mockSatelliteData.getTle()).thenReturn(validTLE);
//        when(satelliteRepository.findById("25544")).thenReturn(Optional.of(mockSatelliteData));
//        ZonedDateTime futureTime = ZonedDateTime.now(ZoneOffset.UTC).plusHours(5);
//        System.out.println("--- FutureLLA Orekit Output ---");
//        satellitePositionService.getFutureLLA(25544, futureTime);
//    }
}