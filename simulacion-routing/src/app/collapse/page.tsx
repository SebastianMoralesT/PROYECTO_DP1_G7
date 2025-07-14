import SimulationMap from "@/components/collapse/SimulationMap";
import Sidebar from "@/components/common/Sidebar";
import StatusBar from "@/components/collapse/statusBar";
import Legend from "@/components/common/Legend";
import { TimeProvider } from "@/components/collapse/TimeContext";

import TransportPanel from "@/components/collapse/TransportPanel";

export default function Home() {
  /*
  return (
    <TimeProvider>
      <div className="flex h-screen bg-gray-100">
        <Sidebar />

        <div className="flex-1 flex flex-col ml-12">
          <StatusBar />

          <div className="flex-1 relative overflow-hidden">
            <SimulationMap />
            <TransportPanel />
            <Legend />
          </div>
        </div>
      </div>
    </TimeProvider>
  );*/
}