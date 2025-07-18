"use client";

import { createContext, useContext, ReactNode, useState } from "react";
import type { Pedido, Camion } from '../../lib/api';

interface TransportContextType {
  activeOrders: Pedido[];
  activeTrucks: Camion[];
  selectedOrder: Pedido | null;
  setActiveOrders: (orders: Pedido[] | ((prev: Pedido[]) => Pedido[])) => void; // Añade soporte para función
  setActiveTrucks: (trucks: Camion[]) => void;
  setSelectedOrder: (order: Pedido | null) => void;
}

const TransportContext = createContext<TransportContextType | undefined>(undefined);

export function TransportProvider({ children }: { children: ReactNode }) {
  const [activeOrders, setActiveOrders] = useState<Pedido[]>([]);
  const [activeTrucks, setActiveTrucks] = useState<Camion[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Pedido | null>(null);
  return (
    <TransportContext.Provider value={{ activeOrders, activeTrucks, selectedOrder, setActiveOrders, setActiveTrucks, setSelectedOrder }}>
      {children}
    </TransportContext.Provider>
  );
}

export function useTransport() {
  const context = useContext(TransportContext);
  if (!context) {
    throw new Error('useTransport must be used within a TransportProvider');
  }
  return context;
}