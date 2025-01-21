import React, { useState } from 'react';
import { Viewer } from 'resium';
import TLEDisplay from './components/TLEDisplay';
import './App.css';
import * as Cesium from 'cesium';

const App = () => {
    // const noradId = 25544; // example norad id for the iss

    console.log("rendering");

    const [noradId, setNoradId] = React.useState(25544);
    const [inputValue, setInputValue] = useState(''); 
    const viewer = React.useRef(null);

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
                <Viewer ref={viewer} homeButton={false}>
                    {/* <button
                        onClick={() => {
                            viewer.current.cesiumElement.camera.flyHome();
                        }}>
                        Home
                    </button> */}
                </Viewer>
            </div>
        </div>
    );
};

export default App;
