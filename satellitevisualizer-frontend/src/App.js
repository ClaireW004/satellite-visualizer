import React, { useState, useRef, useEffect } from 'react';
import { Viewer, Entity } from 'resium';
import * as Cesium from 'cesium';
import TLEDisplay from './components/TLEDisplay';
import cesiumLogo from './cesiumLogo.png';
import axios from 'axios';
import './App.css';
Cesium.Ion.defaultAccessToken = process.env.REACT_APP_CESIUM_ACCESS_TOKEN;
console.log('Access Token:', process.env.REACT_APP_CESIUM_ACCESS_TOKEN);

const App = () => {
    const [noradId, setNoradId] = React.useState(25544)
    const [inputNorad1, setinputNorad1] = useState('');
    const [inputNorad2, setinputNorad2] = useState('');
    const [tle, setTle] = useState("");
    const [currentLLA1, setCurrentLLA1] = useState({ latitude: 0, longitude: 0, altitude: 0 });
    const [currentLLA2, setCurrentLLA2] = useState({ latitude: 0, longitude: 0, altitude: 0 });
    const [error, setError] = useState("");
    const [submitted, setSubmitted] = useState(false);
    const [visibilityResult, setVisibilityResult] = useState("");

    const viewerRef = useRef(null);

    const fetchTLEandLLA = async (noradId, setLLA) => {
        try {
            let response = await axios.get(`http://localhost:8080/api/satellite/${noradId}/tle`);
            console.log(response.data);

            const llaArray = response.data.currentLLA;
            console.log("lla array ", llaArray);
            console.log("lla lat ", llaArray[0][0]);
            console.log("lla lon ", llaArray[0][1]);
            console.log("lla alt ", llaArray[0][2]);
            if (llaArray && llaArray[0].length === 3) {
                setLLA({
                    latitude: llaArray[0][0],
                    longitude: llaArray[0][1],
                    altitude: llaArray[0][2]
                });
            }

            setTle(response.data.tle);
            setError('');
        } catch (err) {
            if (err.response && err.response.status === 404) {
                try {
                    console.log("TLE not found. Fetching and saving new TLE...");
                    await axios.get(`http://localhost:8080/api/satellite/fetch-and-save/${noradId}`);

                    let response = await axios.get(`http://localhost:8080/api/satellite/${noradId}/tle`);
                    console.log("TLE fetched after save:", response.data);

                    const llaArray = response.data.currentLLA;
                    if (llaArray && llaArray[0].length === 3) {
                        setLLA({
                            latitude: llaArray[0][0],
                            longitude: llaArray[0][1],
                            altitude: llaArray[0][2]
                        });
                    }

                    setTle(response.data.tle);
                    setError('');
                } catch (fetchErr) {
                    setError('Failed to fetch and save new TLE data.');
                    console.error(fetchErr);
                }
            } else {
                setError('Failed to fetch TLE data. Satellite not found or server error.');
                console.error(err);
            }
        }
    };

    const handleNoradChange1 = (event) => {
        setinputNorad1(event.target.value);
    };

    const handleNoradChange2 = (event) => {
        setinputNorad2(event.target.value);
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        const parsedId1 = parseInt(inputNorad1, 10);
        const parsedId2 = parseInt(inputNorad2, 10);

        if (!isNaN(parsedId1) && !isNaN(parsedId2)) {
            setSubmitted(true);
            try {
                const viewer = viewerRef.current?.cesiumElement;
                if (viewer) {
                    viewer.dataSources.removeAll();
                    console.log("Cleared Cesium viewer");
                }

                await fetchTLEandLLA(parsedId1, setCurrentLLA1);
                await fetchTLEandLLA(parsedId2, setCurrentLLA2);
                console.log("LLAs updated");

                const res1 = await fetch(`http://localhost:8080/api/satellite/${parsedId1}/czml`);
                const czml1 = await res1.json();

                const czmlSource1 = new Cesium.CzmlDataSource();
                
                if (viewer) {
                    viewer.dataSources.add(czmlSource1);
                    czmlSource1.load(czml1);
                    console.log("loaded czml 1");
                }

                const res2 = await fetch(`http://localhost:8080/api/satellite/${parsedId2}/czml`);
                const czml2 = await res2.json();

                const czmlSource2 = new Cesium.CzmlDataSource();
                if (viewer) {
                    viewer.dataSources.add(czmlSource2);
                    czmlSource2.load(czml2);
                    console.log("loaded czml 2");
                }

                const visibleResponse = await fetch(`http://localhost:8080/api/satellite/${parsedId1}/${parsedId2}/visible-check`);
                const visibleText = await visibleResponse.text();
                setVisibilityResult(visibleText);
                console.log("visibility result fetched")
            } catch (error) {
                console.error("Error fetching LLA:", error);
            }
        } else {
            setSubmitted(false);
        }
    };

    const isLLAValid = (lla) =>
        lla.latitude !== undefined &&
        lla.longitude !== undefined &&
        lla.altitude !== undefined;

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
                            value={inputNorad1}
                            onChange={handleNoradChange1}
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
                            value={inputNorad2}
                            onChange={handleNoradChange2}
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
                <div className="info-container">
                    {submitted ? (
                        isLLAValid(currentLLA1) && isLLAValid(currentLLA2) ? (
                            <div className="lla-display">
                                <h3>Current Geodetic Locations</h3>
                                <div>
                                    <strong>Satellite {inputNorad1 || '1'}:</strong><br />
                                    Lat: {currentLLA1.latitude}<br />
                                    Lon: {currentLLA1.longitude}<br />
                                    Alt: {currentLLA1.altitude} km
                                </div>
                                <br />
                                <div>
                                    <strong>Satellite {inputNorad2 || '2'}:</strong><br />
                                    Lat: {currentLLA2.latitude}<br />
                                    Lon: {currentLLA2.longitude}<br />
                                    Alt: {currentLLA2.altitude} km
                                </div>
                            </div>
                        ) : (
                            <div className="message-box error">
                                Error: Unable to retrieve satellite location data.
                            </div>
                        )
                    ) : (
                        <div className="message-box info">
                            Enter 2 NORAD IDs in to view their geodetic locations.
                        </div>
                    )}
                </div>
                <div className="info-container">
                    <div className="visibility-display">
                        <h3>Visibility Result</h3>
                        <p>{visibilityResult}</p>
                    </div>
                </div>
            </div>
            <div className="viewer-container">
                <Viewer ref={viewerRef} homeButton={false}>

                </Viewer>
            </div>
            <footer>
                <p><em>Made by UF Team Tech 2025</em></p>
            </footer>
        </div >
    );
};

export default App;