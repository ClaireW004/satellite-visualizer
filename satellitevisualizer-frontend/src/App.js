import React from 'react';
import { Viewer } from 'resium';
import TLEDisplay from './components/TLEDisplay';
import * as Cesium from 'cesium';

const App = () => {
    const noradId = 25544; // example norad id for the iss

    console.log("rendering");
    return (
        <div>
            <h1>Satellite Visualizer</h1>
            <div style={{ height: '500px', width: '100%' }} >
                <Viewer full>
                    {/* Cesium objects go here */}
                </Viewer>
            </div>
            <TLEDisplay noradId={noradId} />
        </div>
    );
};

export default App;
