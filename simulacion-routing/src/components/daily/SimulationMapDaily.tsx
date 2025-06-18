// components/daily/SimulationMap.tsx
"use client";

import { BsPlayFill, BsStopFill } from "react-icons/bs";
import { useEffect, useRef, useState, useCallback } from "react";
import { obtenerRutasOptimizadas, obtenerPedidos, obtenerPlantas, obtenerBloqueos } from "../../lib/api";
import type { Camion, SubRuta, Ubicacion, Planta, Pedido, Bloqueo } from '../../lib/api';

export default function SimulationMap() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const truckImgRef = useRef<HTMLImageElement | null>(null);
  const plantPrincipalImgRef = useRef<HTMLImageElement | null>(null);
  const plantSecundariaImgRef = useRef<HTMLImageElement | null>(null);
  const orderImgRef = useRef<HTMLImageElement | null>(null);
  
  // Estado para el tiempo real
  const [currentTime, setCurrentTime] = useState<number>(Date.now());
  const [isRunning, setIsRunning] = useState<boolean>(true);
  const [bloqueos, setBloqueos] = useState<Bloqueo[]>([]);

  // Actualizar el tiempo real cada segundo
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTime(Date.now());
    }, 1000);
    
    return () => clearInterval(interval);
  }, []);

  // Función para dibujar bloqueos
  const drawBloqueos = useCallback((
    ctx: CanvasRenderingContext2D,
    spacing: number,
    currentTime: number
  ) => {
    if (!bloqueos || bloqueos.length === 0) return;

    const bloqueosActivos = bloqueos.filter(bloqueo => {
      try {
        const inicio = new Date(bloqueo.inicio).getTime();
        const fin = new Date(bloqueo.fin).getTime();
        return currentTime >= inicio && currentTime <= fin;
      } catch (e) {
        console.error('Error procesando fechas de bloqueo:', e);
        return false;
      }
    });

    bloqueosActivos.forEach(bloqueo => {
      const nodos = bloqueo.nodos;
      if (nodos.length < 2) return;

      ctx.save();
      ctx.strokeStyle = 'black';
      ctx.lineWidth = 4;
      ctx.beginPath();

      const startX = nodos[0].posX * spacing;
      const startY = nodos[0].posY * spacing;
      ctx.moveTo(startX, startY);

      for (let i = 1; i < nodos.length; i++) {
        const x = nodos[i].posX * spacing;
        const y = nodos[i].posY * spacing;
        ctx.lineTo(x, y);
      }

      ctx.stroke();
      ctx.restore();
    });
  }, [bloqueos]);

  // Función para verificar si un camión debe moverse
  const shouldTruckMove = useCallback((subRutas: SubRuta[], now: number) => {
    return subRutas.some(subRuta => {
      const startTime = new Date(subRuta.horaInicio).getTime();
      return startTime <= now;
    });
  }, []);

  // Función para verificar si un camión ha completado su ruta
  const hasTruckFinished = useCallback((subRutas: SubRuta[], now: number) => {
    if (!subRutas || subRutas.length === 0) return true;
    
    const lastSubRuta = subRutas[subRutas.length - 1];
    const endTime = new Date(lastSubRuta.horaFin).getTime();
    return endTime <= now;
  }, []);

  const [hoveredPlant, setHoveredPlant] = useState<Planta | null>(null);
  const [tooltipPosition, setTooltipPosition] = useState({ x: 0, y: 0 });

  const [imagesLoaded, setImagesLoaded] = useState({
    truck: false,
    plantPrincipal: false,
    plantSecundaria: false,
    order: false
  });
  
  const animationFrameRef = useRef<number>(0);
  const lastTimeRef = useRef<number>(0);
  
  const [trucks, setTrucks] = useState<Camion[]>([]);
  const [plants, setPlants] = useState<Planta[]>([]);
  const [orders, setOrders] = useState<Pedido[]>([]);
  const [routes, setRoutes] = useState<SubRuta[][]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Cargar datos del backend
  useEffect(() => {
    const fetchData = async () => {
      try {
        const [rutasOptimizadas, pedidos, plantas, bloqueosObtenidos] = await Promise.all([
          obtenerRutasOptimizadas(),
          obtenerPedidos(),
          obtenerPlantas(),
          obtenerBloqueos()
        ]);

        const camiones = rutasOptimizadas.map(r => r.camion);
        const subRutas = rutasOptimizadas.map(r => r.subRutas);

        setTrucks(camiones);
        setRoutes(subRutas);
        setOrders(pedidos);
        setPlants(plantas);
        setBloqueos(bloqueosObtenidos);
        setLoading(false);
      } catch (err) {
        setError('Error al cargar los datos de rutas');
        setLoading(false);
        console.error(err);
      }
    };

    fetchData();
  }, []);

  // Cargar imágenes
  useEffect(() => {
    const loadImages = async () => {
      try {
        const loadImage = (src: string) => new Promise<HTMLImageElement>((resolve, reject) => {
          const img = new Image();
          img.src = src;
          img.onload = () => resolve(img);
          img.onerror = reject;
        });

        const [truckImg, plantPrincipalImg, plantSecundariaImg, orderImg] = await Promise.all([
          loadImage('/camionRuta.png'),
          loadImage('/plantaPrincipal.png'),
          loadImage('/plantaSecundaria.png'),
          loadImage('/pedido.png')
        ]);

        truckImgRef.current = truckImg;
        plantPrincipalImgRef.current = plantPrincipalImg;
        plantSecundariaImgRef.current = plantSecundariaImg;
        orderImgRef.current = orderImg;

        setImagesLoaded({
          truck: true,
          plantPrincipal: true,
          plantSecundaria: true,
          order: true
        });
      } catch (error) {
        console.error("Error al cargar las imágenes:", error);
      }
    };
    
    loadImages();
  }, []);

  const drawGrid = useCallback((ctx: CanvasRenderingContext2D, cols: number, rows: number, spacing: number) => {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.strokeStyle = "#ccc";
    
    for (let x = 0; x <= cols; x++) {
      ctx.beginPath();
      ctx.moveTo(x * spacing, 0);
      ctx.lineTo(x * spacing, rows * spacing);
      ctx.stroke();
    }
    for (let y = 0; y <= rows; y++) {
      ctx.beginPath();
      ctx.moveTo(0, y * spacing);
      ctx.lineTo(cols * spacing, y * spacing);
      ctx.stroke();
    }
  }, []);

  const drawPlant = useCallback((
    ctx: CanvasRenderingContext2D, 
    x: number, 
    y: number, 
    plant: Planta, 
    spacing: number
  ) => {
    const isPrincipal = plant.id == "1";
    const img = isPrincipal ? plantPrincipalImgRef.current : plantSecundariaImgRef.current;
    
    if (!img) return;

    const imgSize = isPrincipal ? 30 : 25;
    const canvasX = x * spacing;
    const canvasY = y * spacing;
    
    plant.canvasPosition = { x: canvasX, y: canvasY, size: imgSize };
    
    ctx.save();
    ctx.translate(canvasX, canvasY);
    ctx.rotate(Math.PI);
    ctx.drawImage(img, -imgSize / 2, -imgSize / 2, imgSize, imgSize);
    ctx.restore();

    if (hoveredPlant?.id === plant.id) {
      drawPlantTooltip(ctx, plant, tooltipPosition.x, tooltipPosition.y);
    }
  }, [hoveredPlant, tooltipPosition]);

  const drawPlantTooltip = (
    ctx: CanvasRenderingContext2D,
    plant: Planta,
    x: number,
    y: number
  ) => {
    ctx.save();
    ctx.scale(1, -1);

    const tooltipWidth = 160;
    const tooltipHeight = 55;
    const padding = 10;

    const adjustedX = x + 20 > ctx.canvas.width ? x - tooltipWidth - 10 : x + 12;
    const adjustedY = -y + tooltipHeight > ctx.canvas.height ? -y - 12 : -y + 20;

    ctx.shadowColor = 'rgba(225, 16, 16, 0.3)';
    ctx.shadowBlur = 6;
    ctx.shadowOffsetX = 2;
    ctx.shadowOffsetY = 2;

    ctx.fillStyle = '#ffffff';
    ctx.strokeStyle = '#e0e0e0';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect(adjustedX, adjustedY, tooltipWidth, tooltipHeight, 8);
    ctx.fill();
    ctx.stroke();

    ctx.shadowColor = 'transparent';

    ctx.fillStyle = '#333';
    ctx.font = 'bold 13px Arial';
    ctx.fillText(`Planta ${plant.id}`, adjustedX + padding, adjustedY + 20);

    ctx.fillStyle = '#555';
    ctx.font = '12px Arial';
    ctx.fillText(
      `Capacidad: ${plant.glpDisponible}/${plant.capacidadMaxima}`,
      adjustedX + padding,
      adjustedY + 38
    );

    ctx.restore();
  };

  const drawOrder = useCallback((
    ctx: CanvasRenderingContext2D, 
    x: number, 
    y: number, 
    order: Pedido, 
    spacing: number
  ) => {
    if (!orderImgRef.current) return;

    const imgSize = 20;
    
    ctx.save();
    ctx.translate(x * spacing, y * spacing);
    ctx.rotate(Math.PI);
    ctx.drawImage(orderImgRef.current, -imgSize / 2, -imgSize / 2, imgSize, imgSize);
    ctx.restore();
  }, []);

  const drawRoute = useCallback((
    ctx: CanvasRenderingContext2D,
    route: Ubicacion[],
    color: string,
    spacing: number
  ) => {
    if (!route || route.length === 0) return;

    ctx.save();
    ctx.strokeStyle = color || '#888888';
    ctx.lineWidth = 2;
    ctx.beginPath();
    
    for (let i = 0; i < route.length - 1; i++) {
      const { posX: x1, posY: y1 } = route[i];
      const { posX: x2, posY: y2 } = route[i + 1];
      
      if (i === 0) {
        ctx.moveTo(x1 * spacing, y1 * spacing);
      }
      ctx.lineTo(x2 * spacing, y2 * spacing);
    }
    
    ctx.stroke();
    
    ctx.fillStyle = color || '#888888';
    route.forEach(({ posX: x, posY: y }) => {
      ctx.beginPath();
      ctx.arc(x * spacing, y * spacing, 3, 0, Math.PI * 2);
      ctx.fill();
    });
    
    ctx.restore();
  }, []);

  const drawTruck = useCallback((
    ctx: CanvasRenderingContext2D, 
    x: number, 
    y: number, 
    truck: Camion, 
    spacing: number,
    targetPos?: [number, number], 
    currentPos?: [number, number],
    isFinalPosition: boolean = false
  ) => {
    if (!truckImgRef.current) return;

    const img = truckImgRef.current;
    const imgSize = 20;

    ctx.save();
    ctx.translate(x * spacing, y * spacing);

    if (targetPos && currentPos) {
      const dx = targetPos[0] - currentPos[0];
      const dy = targetPos[1] - currentPos[1];

      if (isFinalPosition) {
        ctx.rotate(Math.PI);  
      } else {
        if (dx === 1 && dy === 0) {
          ctx.rotate(Math.PI);
        } else if (dx === -1 && dy === 0) {
          ctx.scale(1, -1);
          ctx.rotate(0);
        } else if (dx === 0 && dy === 1) {
          ctx.rotate(3 * Math.PI / 2);
        } else if (dx === 0 && dy === -1) {
          ctx.rotate(Math.PI / 2);
        }
      }
    }

    ctx.drawImage(img, -imgSize / 2, -imgSize / 2, imgSize, imgSize);
    ctx.restore();

    // Dibujar ID
    ctx.save();
    ctx.scale(1, -1);
    ctx.fillStyle = '#000000';
    ctx.font = '10px Arial';
    ctx.fillText(truck.codigo, x * spacing - 5, -y * spacing + 5);
    ctx.restore();
  }, []);

  const animate = useCallback((timestamp: number) => {
    if (!canvasRef.current || !Object.values(imagesLoaded).every(Boolean)) return;
    
    const ctx = canvasRef.current.getContext("2d");
    if (!ctx) return;

    const cols = 70;
    const rows = 50;
    const spacing = 13;

    if (!lastTimeRef.current) {
      lastTimeRef.current = timestamp;
    }
    
    drawGrid(ctx, cols, rows, spacing);
    drawBloqueos(ctx, spacing, currentTime);
    
    // Dibujar plantas
    plants.forEach(plant => {
      drawPlant(ctx, plant.ubicacion.posX, plant.ubicacion.posY, plant, spacing);
    });

    // Dibujar pedidos
    orders.forEach(order => {
      drawOrder(ctx, order.destino.posX, order.destino.posY, order, spacing);
    });

    let anyTruckActive = false;

    routes.forEach((subRutas, index) => {
      const truck = trucks[index];
      if (!truck || !subRutas || subRutas.length === 0) return;

      const shouldMove = shouldTruckMove(subRutas, currentTime);
      const hasFinished = hasTruckFinished(subRutas, currentTime);

      if (!shouldMove) {
        // Dibujar camión en posición inicial
        drawTruck(
          ctx,
          truck.ubicacionActual.posX, 
          truck.ubicacionActual.posY, 
          truck,
          spacing
        );
        return;
      }

      if (hasFinished) {
        // Dibujar camión en posición final
        const finalPos = subRutas[subRutas.length - 1].trayectoria.slice(-1)[0];
        drawTruck(
          ctx,
          finalPos.posX, 
          finalPos.posY, 
          truck,
          spacing,
          undefined,
          undefined,
          true
        );
        return;
      }

      anyTruckActive = true;
      
      // Filtrar subrutas que ya deberían estar activas
      const activeSubRutas = subRutas.filter(subRuta => 
        new Date(subRuta.horaInicio).getTime() <= currentTime
      );
      
      const fullRoute = activeSubRutas.flatMap(subRuta => subRuta.trayectoria);
      
      // Calcular progreso basado en el tiempo real
      const routeStartTime = new Date(subRutas[0].horaInicio).getTime();
      const routeEndTime = new Date(subRutas[subRutas.length - 1].horaFin).getTime();
      const totalDuration = routeEndTime - routeStartTime;
      const elapsedTime = currentTime - routeStartTime;
      const progress = Math.min(elapsedTime / totalDuration, 1);
      
      // Calcular posición actual
      const totalSteps = fullRoute.length;
      const currentStep = Math.min(Math.floor(progress * (totalSteps - 1)), totalSteps - 2);
      const stepProgress = (progress * (totalSteps - 1)) % 1;
      
      if (currentStep < fullRoute.length - 1) {
        const currentPos = fullRoute[currentStep];
        const nextPos = fullRoute[currentStep + 1];
        
        const interpolatedX = currentPos.posX + (nextPos.posX - currentPos.posX) * stepProgress;
        const interpolatedY = currentPos.posY + (nextPos.posY - currentPos.posY) * stepProgress;
        
        drawTruck(
          ctx,
          interpolatedX,
          interpolatedY,
          truck,
          spacing,
          [nextPos.posX, nextPos.posY],
          [currentPos.posX, currentPos.posY]
        );
      } else {
        // Última posición
        const finalPos = fullRoute[fullRoute.length - 1];
        drawTruck(
          ctx,
          finalPos.posX,
          finalPos.posY,
          truck,
          spacing,
          undefined,
          undefined,
          true
        );
      }
    });

    if (isRunning && anyTruckActive) {
      animationFrameRef.current = requestAnimationFrame(animate);
    }
  }, [currentTime, isRunning, imagesLoaded, plants, orders, trucks, routes, drawGrid, drawBloqueos, drawPlant, drawOrder, drawTruck, shouldTruckMove, hasTruckFinished]);

  const drawInitialState = useCallback(() => {
    if (!canvasRef.current || !Object.values(imagesLoaded).every(Boolean)) return;
    
    const ctx = canvasRef.current.getContext("2d");
    if (!ctx) return;

    const cols = 70;
    const rows = 50;
    const spacing = 13;

    canvasRef.current.width = cols * spacing;
    canvasRef.current.height = rows * spacing;

    ctx.translate(0, canvasRef.current.height);
    ctx.scale(1, -1);

    drawGrid(ctx, cols, rows, spacing);
    drawBloqueos(ctx, spacing, currentTime);
    
    plants.forEach(plant => {
      drawPlant(ctx, plant.ubicacion.posX, plant.ubicacion.posY, plant, spacing);
    });

    orders.forEach(order => {
      drawOrder(ctx, order.destino.posX, order.destino.posY, order, spacing);
    });

    trucks.forEach((truck) => {
      drawTruck(
        ctx,
        truck.ubicacionActual.posX,
        truck.ubicacionActual.posY,
        truck,
        spacing
      );
    });
  }, [imagesLoaded, trucks, plants, orders, drawGrid, drawTruck, drawPlant, drawOrder, drawBloqueos, currentTime]);

  const startAnimation = useCallback(() => {
    cancelAnimationFrame(animationFrameRef.current);
    lastTimeRef.current = 0;
    animationFrameRef.current = requestAnimationFrame(animate);
  }, [animate]);

  const stopAnimation = useCallback(() => {
    cancelAnimationFrame(animationFrameRef.current);
  }, []);

  useEffect(() => {
    if (!Object.values(imagesLoaded).every(Boolean) || !canvasRef.current) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const cols = 70;
    const rows = 50;
    const spacing = 13;

    canvas.width = cols * spacing;
    canvas.height = rows * spacing;

    ctx.translate(0, canvas.height);
    ctx.scale(1, -1);

    drawGrid(ctx, cols, rows, spacing);
    drawBloqueos(ctx, spacing, currentTime);
    
    plants.forEach(plant => {
      drawPlant(ctx, plant.ubicacion.posX, plant.ubicacion.posY, plant, spacing);
    });

    orders.forEach(order => {
      drawOrder(ctx, order.destino.posX, order.destino.posY, order, spacing);
    });
    
    trucks.forEach((truck) => {
      drawTruck(
        ctx,
        truck.ubicacionActual.posX,
        truck.ubicacionActual.posY,
        truck,
        spacing
      );
    });
  }, [imagesLoaded, trucks, plants, orders, drawGrid, drawTruck, drawPlant, drawOrder, drawBloqueos, currentTime]);

  const handleCanvasHover = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!canvasRef.current) return;
    
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = rect.bottom - e.clientY;
    
    setTooltipPosition({ x, y });
    
    const hovered = plants.find(plant => {
      if (!plant.canvasPosition) return false;
      const { x: plantX, y: plantY, size } = plant.canvasPosition;
      return x >= plantX - size/2 && 
             x <= plantX + size/2 && 
             y >= plantY - size/2 && 
             y <= plantY + size/2;
    });
    
    setHoveredPlant(hovered || null);
  };

  useEffect(() => {
    if (isRunning) {
      startAnimation();
    } else {
      stopAnimation();
    }
    
    return () => {
      stopAnimation();
    };
  }, [isRunning, startAnimation, stopAnimation]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-200 flex items-center justify-center">
        <div className="text-xl">Cargando datos...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-200 flex items-center justify-center">
        <div className="text-xl text-red-500">{error}</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-200 relative overflow-auto">
      <div className="absolute inset-0 flex items-center justify-center overflow-auto">
        <canvas 
          ref={canvasRef} 
          className="bg-white border border-gray-400"
          onMouseMove={handleCanvasHover}
          onMouseOut={() => setHoveredPlant(null)}
        />
      </div>
    </div>
  );
}