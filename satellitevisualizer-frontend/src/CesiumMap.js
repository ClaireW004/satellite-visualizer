import React, { useEffect} from "react";
import { Viewer, Entity } from "resium";
import * as Cesium from "cesium";

const CesiumMap = () => {
    const viewer = React.useRef(null);

    // useEffect(() => {
    //     if (viewer.current) {
    //         viewer.current.cesiumElement.camera.flyTo({
    //             destination: Cesium.Cartesian3.fromDegrees(-82.34435030956476, 29.6482998, 1000),
    //         });
    //     }
    // }, []);

    return (
        <div className="viewer-container">
            <Viewer ref={viewer} homeButton={false} full>
             
            </Viewer>
        </div>
    );
};

export default CesiumMap;
