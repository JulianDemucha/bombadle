import React, { useEffect } from 'react';
import { useWindowSize } from 'react-use';
import confetti from 'canvas-confetti';

const WinAnimation = ({ isVisible }) => {
    const { width, height } = useWindowSize();

    useEffect(() => {
        if (isVisible) {
            const count = 200;
            const defaults = {
                origin: { y: 0.7 }
            };

            const fire = (particleRatio, opts) => {
                confetti({
                    ...defaults,
                    ...opts,
                    particleCount: Math.floor(count * particleRatio)
                });
            };

            // Strzał z lewej
            fire(0.25, {
                spread: 50,
                startVelocity: 55,
                origin: { x: 0, y: 1 },
                angle: 60
            });
            // Strzał z prawej
            fire(0.25, {
                spread: 50,
                startVelocity: 55,
                origin: { x: 1, y: 1 },
                angle: 120
            });
            // Drugi strzał lewy
            fire(0.2, {
                spread: 60,
                startVelocity: 45,
                origin: { x: 0, y: 1 },
                angle: 45
            });
            // Drugi strzał prawy
            fire(0.2, {
                spread: 60,
                startVelocity: 45,
                origin: { x: 1, y: 1 },
                angle: 135
            });
            // Mały deszcz środkiem
            fire(0.1, {
                spread: 120,
                decay: 0.91,
                scalar: 0.8,
                origin: { x: 0.5, y: 1 },
                startVelocity: 60,
                angle: 90
            });
        }
    }, [isVisible]);

    return null;
};

export default WinAnimation;
