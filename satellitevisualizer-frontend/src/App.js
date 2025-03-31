import React, { useState, useRef, useEffect } from 'react';
    import { Viewer, Entity } from 'resium';
    import * as Cesium from 'cesium';
    import TLEDisplay from './components/TLEDisplay';
    import './App.css';
    const App = () => {
        // const noradId = 25544; // example norad id for the iss
        const [noradId, setNoradId] = React.useState(25544);
        const [inputValue, setInputValue] = useState('');
        const viewerRef = useRef(null);
        let altitude = 20297;
        let latitude = "14.3*S";
        let longitude = "12.3*W";
        let magnitude = "Unknown";
        let distance = 28563
        let alt = -24;
        let az = 100;
        const handleInputChange = (event) => {
            setInputValue(event.target.value);
        };
        const handleSubmit = (event) => {
            event.preventDefault();
            const parsedId = parseInt(inputValue, 10);
            if (!isNaN(parsedId)) {
                setNoradId(parsedId);
            }
        };
        return (
            <div className="app-container">
                <div className="sidebar">
                    <h1>Satellite Visualizer</h1>
                    <form onSubmit={handleSubmit}>
                        <input
                            type="text"
                            value={inputValue}
                            onChange={handleInputChange}
                            placeholder="Enter NORAD ID"
                        />
                        <button type="submit">Submit</button>
                    </form>
                    <TLEDisplay noradId={noradId} />
                </div>
                <div className="viewer-container">
                    <Viewer ref={viewerRef} homeButton={false} infoBox ={true}>
                        <Entity
                            name="Test Point 1"
                            position={Cesium.Cartesian3.fromDegrees(-82.34435030956476, 29.6482998, 31)}
                            point={{ pixelSize: 10,
                                color: Cesium.Color.RED,
                                outlineColor: Cesium.Color.WHITE,
                                outlineWidth: 2 }}
                                description={`Position: <br /> 
                                  Altitude: ${altitude} km <br /> 
                                  Lat: ${latitude} <br /> 
                                  Long: ${longitude} <br /> 
                                  Magnitude: ${magnitude} <br /> 
                                  Distance: ${distance} km <br /> 
                                  Alt: ${alt}° <br /> 
                                  Az: ${az}°`}
                                >
                        </Entity>
                        <Entity
                            name="Test Point 2"
                            position={Cesium.Cartesian3.fromDegrees(2.2955342, 48.8580382, 41)}
                            point={{ pixelSize: 10,
                                color: Cesium.Color.RED,
                                outlineColor: Cesium.Color.WHITE,
                                outlineWidth: 2 }}
                                description={`Position: <br /> 
                                  Altitude: ${altitude} km <br /> 
                                  Lat: ${latitude} <br /> 
                                  Long: ${longitude} <br /> 
                                  Magnitude: ${magnitude} <br /> 
                                  Distance: ${distance} km <br /> 
                                  Alt: ${alt}° <br /> 
                                  Az: ${az}°`}>
                        </Entity>
                        
                    </Viewer>
                </div>
            </div>
        );
    };
    export default App;