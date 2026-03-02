declare module 'heatmap.js' {
    interface HeatmapConfiguration {
        container: HTMLElement;
        radius?: number;
        maxOpacity?: number;
        minOpacity?: number;
        blur?: number;
        gradient?: {
            [key: string]: string;
        };
    }

    interface DataPoint {
        x: number;
        y: number;
        value: number;
        radius?: number;
    }

    interface HeatmapInstance {
        setData(data: { max: number; data: DataPoint[] }): void;
        addData(dataPoint: DataPoint): void;
        getValueAt(point: { x: number; y: number }): number;
        repaint(): void;
    }

    export default {
        create(config: HeatmapConfiguration): HeatmapInstance;
    };
} 