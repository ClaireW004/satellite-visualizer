import React, { useState, useRef, useEffect } from 'react';
import { Viewer, Entity } from 'resium';
import * as Cesium from 'cesium';
import TLEDisplay from './components/TLEDisplay';
import cesiumLogo from './cesiumLogo.png';
import axios from 'axios';
import './App.css';

const App = () => {
    // const noradId = 25544; // example norad id for the iss
    const [noradId, setNoradId] = React.useState(25544)
    const [inputNorad1, setinputNorad1] = useState('');
    const [inputNorad2, setinputNorad2] = useState('');
    const [tle, setTle] = useState("");
    const [currentLLA1, setCurrentLLA1] = useState({ latitude: undefined, longitude: undefined, altitude: undefined });
    const [currentLLA2, setCurrentLLA2] = useState({ latitude: undefined, longitude: undefined, altitude: undefined });
    const [error, setError] = useState("");
    const viewerRef = useRef(null);

    const fetchTLEandLLA = async (noradId, setLLA) => {
        try {
            const response = await axios.get(`http://localhost:8080/api/satellite/${noradId}/tle`);
            console.log(response.data);

            const llaArray = response.data.currentLLA;
            if (Array.isArray(llaArray)) {
                const llaObject = {
                    latitude: llaArray[0],
                    longitude: llaArray[1],
                    altitude: llaArray[2]
                };
                setLLA(llaObject);
            } else {
                setLLA(response.data.currentLLA);
            }

            setTle(response.data.tle);
            setError('');
        } catch (err) {
            setError('Failed to fetch TLE data. Satellite not found or server error.');
        }
    };

    const handleNoradChange1 = (event) => {
        setinputNorad1(event.target.value);
    };

    const handleNoradChange2 = (event) => {
        setinputNorad2(event.target.value);
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        const parsedId1 = parseInt(inputNorad1, 10);
        const parsedId2 = parseInt(inputNorad2, 10);
        if (!isNaN(parsedId1) && !isNaN(parsedId2)) {
            fetchTLEandLLA(parsedId1, setCurrentLLA1);
            fetchTLEandLLA(parsedId2, setCurrentLLA2);
            console.log(parsedId1);
            console.log(parsedId2);
        }
    };

    return (
        <div className="app-container">
            <div className="sidebar">
                <div className="logo-title">
                    <img src={cesiumLogo} alt="Logo" className="sidebar-logo" />
                    <div>
                        <h1 className="satellite-text">SATELLITE<br /></h1>
                        <h1 className="visualizer-text">VISUALIZER</h1>
                    </div>
                </div>
                <form onSubmit={handleSubmit}>
                    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginTop: "20px" }}>
                        <input
                            type="text"
                            placeholder="Enter NORAD ID 1"
                            style={{
                                margin: "10px",
                                padding: "10px",
                                border: "2px solid #fff",
                                borderRadius: "15px",
                                outline: "none",
                                width: "250px",
                            }}
                        />
                        <input
                            type="text"
                            placeholder="Enter NORAD ID 2"
                            style={{
                                margin: "10px",
                                padding: "10px",
                                border: "2px solid #fff",
                                borderRadius: "15px",
                                outline: "none",
                                width: "250px",
                            }}
                        />
                        <div style={{ textAlign: "center", width: "100%" }}>
                            <button
                                style={{
                                    margin: "10px",
                                    padding: "10px 20px",
                                    border: "2px solid #000",
                                    borderRadius: "15px",
                                    backgroundColor: "#fff",
                                    cursor: "pointer",
                                }}
                            >
                                Submit
                            </button>
                        </div>
                    </div>
                </form>
                <TLEDisplay noradId={noradId} />
                {currentLLA1.length > 0 && currentLLA2.length > 0 && (
                    <div style={{ color: "white" }}>
                        <h3>Current LLA:</h3>
                        <div>
                            <strong>Satellite 1:</strong>
                            Lat: {(currentLLA1.latitude)},
                            Lon: {currentLLA1.longitude},
                            Alt: {currentLLA1.altitude} km
                        </div>
                        <div>
                            <strong>Satellite 2:</strong>
                            Lat: {(currentLLA2[0])},
                            Lon: {(currentLLA2[1])},
                            Alt: {(currentLLA2[2])} km
                        </div>
                    </div>
                )}

            </div>
            <div className="viewer-container">
                <Viewer ref={viewerRef} homeButton={false}>
                    {currentLLA1.longitude !== undefined && (
                        <Entity
                            name="Satellite 1"
                            position={Cesium.Cartesian3.fromDegrees(
                                currentLLA1.longitude,
                                currentLLA1.latitude,
                                currentLLA1.altitude * 1000 // km to meters
                            )}
                            point={{
                                pixelSize: 10,
                                color: Cesium.Color.RED,
                                outlineColor: Cesium.Color.WHITE,
                                outlineWidth: 2
                            }}
                            description={`Position:<br />
                          Latitude: ${currentLLA1.latitude}°<br />
                          Longitude: ${currentLLA1.longitude}°<br />
                          Altitude: ${currentLLA1.altitude} km`}
                        />
                    )}

                    {currentLLA2.longitude !== undefined && (
                        <Entity
                            name="Satellite 2"
                            position={Cesium.Cartesian3.fromDegrees(
                                currentLLA2.longitude,
                                currentLLA2.latitude,
                                currentLLA2.altitude * 1000
                            )}
                            point={{
                                pixelSize: 10,
                                color: Cesium.Color.BLUE,
                                outlineColor: Cesium.Color.WHITE,
                                outlineWidth: 2
                            }}
                            description={`Position: <br /> 
                    Altitude: ${currentLLA2.altitude} km <br /> 
                    Lat: ${currentLLA2.latitude} <br /> 
                    Long: ${currentLLA2.longitude}`}
                        />
                    )}
                </Viewer>
            </div>
        </div >
    );
};

export default App;