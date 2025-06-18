// components/common/Legend.tsx
'use client';
import { useState } from "react";
import { FiChevronDown, FiChevronUp, FiX } from "react-icons/fi";

export default function Legend() {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(true);

  return (
    <>
      {/* Botón flotante cuando está cerrado */}
      {!isOpen && (
        <button 
          onClick={() => setIsOpen(true)}
          className="fixed left-16 bottom-4 bg-red-500 text-white p-2 rounded-r-lg shadow-lg z-30 flex items-center"
        >
          <span className="mr-1">Leyenda</span>
          <FiChevronUp size={16} />
        </button>
      )}

      {/* Panel de leyenda */}
      <div className={`fixed left-16 bottom-4 bg-white border shadow-lg rounded-lg z-20 transition-all duration-300 ${isOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
           style={{ width: isExpanded ? '220px' : '160px' }}>
        
        {/* Header */}
        <div className="bg-red-500 text-white p-2 rounded-t-lg flex justify-between items-center">
          <h3 className="font-semibold text-sm">Leyenda</h3>
          <div className="flex space-x-2">
            
            <button 
              onClick={() => setIsOpen(false)}
              className="text-white hover:text-red-200"
              title="Cerrar"
            >
              <FiX size={16} />
            </button>
          </div>
        </div>

        {/* Contenido */}
        <div className="p-3">
          <ul className="text-xs space-y-1">
            <li className="flex items-center"><span className="mr-2">🚚</span> {isExpanded && 'Camión en tránsito'}</li>
            <li className="flex items-center"><span className="mr-2">🚧</span> {isExpanded && 'Camión averiado'}</li>
            <li className="flex items-center"><span className="mr-2">📍</span> {isExpanded && 'Punto de entrega'}</li>
            {isExpanded && (
              <>
                <li className="flex items-center"><span className="mr-2">🏭</span> Tanque Principal</li>
                <li className="flex items-center"><span className="mr-2">🛢️</span> Tanque Intermedio</li>
                <li className="flex items-center"><span className="mr-2">🛑</span> Bloqueo de ruta</li>
              </>
            )}
          </ul>
        </div>
      </div>
    </>
  );
}