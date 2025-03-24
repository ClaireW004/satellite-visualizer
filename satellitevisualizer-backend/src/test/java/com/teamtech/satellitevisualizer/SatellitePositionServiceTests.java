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

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SatellitePositionServiceTest {

    @Mock
    private SatelliteRepository satelliteRepository;

    @InjectMocks
    private SatellitePositionService satellitePositionService;

    private final String validTLE =
            "1 25544U 98067A   25048.86150170  .00016610  00000-0  29603-3 0  9994\n" +
                    "2 25544  51.6391 178.0267 0004105 330.1473 172.8419 15.50222990496656";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create mock SatelliteData object
        SatelliteData mockSatelliteData = mock(SatelliteData.class);
        when(mockSatelliteData.getTle()).thenReturn(validTLE);

        // Mock repository to return mockSatelliteData when ID "25544" is requested
        when(satelliteRepository.findById("25544")).thenReturn(Optional.of(mockSatelliteData));
    }

    @Test
    void testParseTLE() {
        Optional<TLE> tleOptional = SatellitePositionService.parseTLE(validTLE);
        assertTrue(tleOptional.isPresent(), "TLE should be parsed correctly");

        TLE tle = tleOptional.get();
        assertEquals("1 25544U 98067A   25048.86150170  .00016610  00000-0  29603-3 0  9994", tle.getLine1());
        assertEquals("2 25544  51.6391 178.0267 0004105 330.1473 172.8419 15.50222990496656", tle.getLine2());
    }
}