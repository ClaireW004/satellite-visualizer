import React, { useState } from 'react';
import { Viewer } from 'resium';
import TLEDisplay from './components/TLEDisplay';
import './App.css';
import * as Cesium from 'cesium';
import axios from 'axios';

const App = () => {
    // const noradId = 25544; // example norad id for the iss

    console.log("rendering");

    const [noradId, setNoradId] = React.useState(25544);
    const [inputValue, setInputValue] = useState('');
    const [tle, setTle] = useState("");
    const [currentLLA, setCurrentLLA] = useState([]);
    const [error, setError] = useState(""); 
    const viewer = React.useRef(null);

    const fetchTLEandLLA = async () => {
        try {
            const response = await axios.get(`http://localhost:8080/api/satellite/${noradId}/tle`);
            console.log(response.data);
            setTle(response.data.tle);
            setCurrentLLA(response.data.currentLLA);
            setError('');
        } catch (err) {
            setError('Failed to fetch TLE data. Satellite not found or server error.');
            setTle('');
            setCurrentLLA([]);
        }
    };

    const handleInputChange = (event) => { 
        setInputValue(event.target.value); 
    }; 
    
    const handleSubmit = (event) => { 
        event.preventDefault(); 
        const parsedId = parseInt(inputValue, 10); 
        if (!isNaN(parsedId)) { 
            setNoradId(parsedId); 
            fetchTLEandLLA(noradId);
        }
        
    };

    return (
        <div className="app-container">
            <div className="sidebar">
                <h1>Satellite Visualizer</h1>
                <form onSubmit={handleSubmit} className='input-group'> 
                    <input 
                        type="text" 
                        value={inputValue} 
                        onChange={handleInputChange} 
                        placeholder="Enter NORAD ID" 
                    /> 
                    <button type="submit">Submit</button> 
                </form>
                <TLEDisplay  tle={tle} error={error}/>
                {currentLLA.length > 0 && (
                <div>
                    <h3>Current LLA:</h3>
                    {currentLLA.map((row, rowIndex) => (
                        <div key={rowIndex}>
                            Row {rowIndex + 1}: {row.join(", ")}
                        </div>
                    ))}
                </div>
            )}

            </div>
            <div className="viewer-container">
                <Viewer ref={viewer} homeButton={false}>
                    {/* Add other Cesium components here
                    */}
                </Viewer>
            </div>
        </div>
    );
};

export default App;
