import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { EquipmentStatusBadge } from '../components/common/StatusBadge';
import { connectEquipmentStatusWebSocket } from '../api/websocket';
import {
  Activity,
  Clock,
  Wifi,
  WifiOff,
  Filter,
  RefreshCw,
  Search,
  MapPin,
  ChevronRight,
  TrendingUp,
  AlertTriangle,
  UserCheck,
  Calendar
} from 'lucide-react';
import { format } from 'date-fns';

export default function LiveMonitoring() {
  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(new Date());

  // Filter states
  const [statusFilter, setStatusFilter] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');

  const wsClientRef = useRef(null);

  const fetchLiveStatus = () => {
    setLoading(true);
    api.get('/equipment/live-status')
      .then((res) => {
        if (res.success && res.data) {
          setEquipmentList(res.data);
          setLastUpdated(new Date());
          setError('');
        }
      })
      .catch((err) => {
        setError(err.message || 'Failed to fetch real-time equipment status.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchLiveStatus();

    // Initialize WebSocket connection for live status broadcasts
    const ws = connectEquipmentStatusWebSocket({
      onConnect: () => {
        setIsConnected(true);
        // Refresh data on connect/reconnect to prevent desync
        fetchLiveStatus();
      },
      onDisconnect: () => {
        setIsConnected(false);
      },
      onMessage: (message) => {
        setLastUpdated(new Date());
        setEquipmentList((prev) =>
          prev.map((eq) => {
            if (eq.equipmentId === message.equipmentId) {
              return {
                ...eq,
                equipmentStatus: message.status,
                inUse: message.inUse,
                sessionStartTime: message.currentSessionStartTime,
                scheduledEndTime: message.scheduledEndTime,
                elapsedSeconds: 0,
                elapsedMinutes: 0,
              };
            }
            return eq;
          })
        );
      },
    });

    wsClientRef.current = ws;

    // Timer to update elapsed seconds on active sessions every 5 seconds
    const timer = setInterval(() => {
      setEquipmentList((prev) =>
        prev.map((eq) => {
          if (eq.inUse && eq.sessionStartTime) {
            const start = new Date(eq.sessionStartTime).getTime();
            const now = Date.now();
            const elapsed = Math.max(0, Math.floor((now - start) / 1000));
            return {
              ...eq,
              elapsedSeconds: elapsed,
              elapsedMinutes: Math.floor(elapsed / 60),
            };
          }
          return eq;
        })
      );
    }, 5000);

    return () => {
      clearInterval(timer);
      if (wsClientRef.current) {
        wsClientRef.current.disconnect();
      }
    };
  }, []);

  const filteredList = equipmentList.filter((item) => {
    if (statusFilter && item.equipmentStatus !== statusFilter) {
      return false;
    }
    if (selectedCategory && item.category !== selectedCategory) {
      return false;
    }
    if (searchKeyword) {
      const kw = searchKeyword.toLowerCase();
      const matchName = item.equipmentName?.toLowerCase().includes(kw);
      const matchTag = item.assetTag?.toLowerCase().includes(kw);
      const matchRoom = item.locationRoom?.toLowerCase().includes(kw);
      if (!matchName && !matchTag && !matchRoom) return false;
    }
    return true;
  });

  const categories = Array.from(new Set(equipmentList.map((e) => e.category).filter(Boolean)));

  const totalMachines = equipmentList.length;
  const inUseCount = equipmentList.filter((e) => e.inUse).length;
  const availableCount = equipmentList.filter((e) => e.equipmentStatus === 'AVAILABLE' && !e.inUse).length;
  const maintenanceCount = equipmentList.filter(
    (e) => e.equipmentStatus === 'UNDER_MAINTENANCE' || e.equipmentStatus === 'OUT_OF_SERVICE'
  ).length;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <div className="flex items-center space-x-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Live Equipment Utilization Monitor
            </h1>
            <span
              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                isConnected
                  ? 'bg-emerald-100 text-emerald-800'
                  : 'bg-amber-100 text-amber-800'
              }`}
            >
              {isConnected ? (
                <>
                  <Wifi className="w-3 h-3 mr-1 text-emerald-600 animate-pulse" /> Live Stream Connected
                </>
              ) : (
                <>
                  <WifiOff className="w-3 h-3 mr-1 text-amber-600" /> Reconnecting Stream...
                </>
              )}
            </span>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            Real-time status broadcast of laboratory instruments, active sessions, and upcoming reservations.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            to="/analytics"
            className="inline-flex items-center px-4 py-2 bg-indigo-700 hover:bg-indigo-800 text-white text-xs font-semibold rounded-xl shadow-sm transition"
          >
            <TrendingUp className="w-4 h-4 mr-1.5" />
            Utilization Analytics & Heatmaps
          </Link>
          <button
            onClick={fetchLiveStatus}
            className="inline-flex items-center px-3.5 py-2 bg-white hover:bg-slate-50 border border-slate-300 text-slate-700 text-xs font-semibold rounded-xl shadow-sm transition"
          >
            <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${loading ? 'animate-spin text-teal-600' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* KPI Metric Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-slate-500">Total Tracked Machines</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{totalMachines}</p>
          </div>
          <div className="p-3 bg-slate-100 rounded-xl text-slate-600">
            <Activity className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-emerald-600">Currently In Use</p>
            <p className="text-2xl font-bold text-emerald-700 mt-1">{inUseCount}</p>
          </div>
          <div className="p-3 bg-emerald-50 rounded-xl text-emerald-600">
            <Clock className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-teal-600">Available For Booking</p>
            <p className="text-2xl font-bold text-teal-700 mt-1">{availableCount}</p>
          </div>
          <div className="p-3 bg-teal-50 rounded-xl text-teal-600">
            <UserCheck className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-amber-600">Under Service / Fault</p>
            <p className="text-2xl font-bold text-amber-700 mt-1">{maintenanceCount}</p>
          </div>
          <div className="p-3 bg-amber-50 rounded-xl text-amber-600">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by equipment name, asset tag, or room..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-600 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-teal-600 focus:outline-none"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-xl text-xs font-medium text-slate-700 bg-white focus:ring-2 focus:ring-teal-600 focus:outline-none"
          >
            <option value="">All Statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="BOOKED">Booked / In Use</option>
            <option value="UNDER_MAINTENANCE">Under Maintenance</option>
            <option value="OUT_OF_SERVICE">Out of Service</option>
            <option value="RETIRED">Retired</option>
          </select>
        </div>
      </div>

      {/* Main Grid View */}
      {loading && equipmentList.length === 0 ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-600"></div>
        </div>
      ) : error ? (
        <div className="p-6 bg-rose-50 border border-rose-200 text-rose-700 rounded-2xl text-center text-sm space-y-3">
          <p>{error}</p>
          <button
            onClick={fetchLiveStatus}
            className="px-4 py-2 bg-rose-600 text-white text-xs font-semibold rounded-xl hover:bg-rose-700 transition"
          >
            Retry Connection
          </button>
        </div>
      ) : filteredList.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <Activity className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">No Instruments Match Filter</h3>
          <p className="text-sm text-slate-500 mt-1">Adjust search parameters or clear filters.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredList.map((item) => {
            const hasActiveSession = item.inUse;

            return (
              <div
                key={item.equipmentId}
                className={`bg-white rounded-2xl border transition-all duration-200 overflow-hidden shadow-sm flex flex-col justify-between ${
                  hasActiveSession
                    ? 'border-emerald-300 ring-2 ring-emerald-100'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="p-5 space-y-3">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[11px] font-mono font-semibold px-2 py-0.5 bg-slate-100 text-slate-700 rounded">
                        {item.assetTag}
                      </span>
                      <h3 className="text-base font-bold text-slate-900 mt-1.5 leading-snug">
                        <Link to={`/equipment/${item.equipmentId}`} className="hover:text-teal-700 transition">
                          {item.equipmentName}
                        </Link>
                      </h3>
                      <p className="text-xs text-slate-500">{item.category}</p>
                    </div>

                    <div className="flex flex-col items-end gap-1">
                      <EquipmentStatusBadge status={item.equipmentStatus} />
                      {hasActiveSession && (
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 animate-pulse">
                          ● ACTIVE NOW
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="text-xs text-slate-600 flex items-center space-x-2 pt-1 border-t border-slate-100">
                    <MapPin className="w-3.5 h-3.5 text-slate-400" />
                    <span>{item.locationRoom || 'Main Lab Suite'}</span>
                    <span className="text-slate-300">•</span>
                    <span>{item.departmentName}</span>
                  </div>

                  {/* Active Session Info Panel */}
                  {hasActiveSession && (
                    <div className="p-3 bg-emerald-50/70 border border-emerald-200 rounded-xl space-y-1.5">
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-emerald-800 font-semibold">Active Session Started:</span>
                        <span className="font-mono text-emerald-900">
                          {item.sessionStartTime
                            ? format(new Date(item.sessionStartTime), 'h:mm a')
                            : 'In progress'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-emerald-800 font-semibold">Elapsed Duration:</span>
                        <span className="font-mono font-bold text-emerald-900">
                          {item.elapsedMinutes != null ? `${item.elapsedMinutes} mins` : 'Counting...'}
                        </span>
                      </div>
                      {item.scheduledEndTime && (
                        <div className="flex items-center justify-between text-xs">
                          <span className="text-emerald-700">Scheduled End:</span>
                          <span className="font-mono text-emerald-800">
                            {format(new Date(item.scheduledEndTime), 'h:mm a')}
                          </span>
                        </div>
                      )}
                      {/* Manager-only authorized detail */}
                      {item.currentUserName && (
                        <div className="pt-1.5 border-t border-emerald-200 text-[11px] text-emerald-900">
                          <span className="font-semibold">Operator:</span> {item.currentUserName}
                          {item.currentBookingPurpose && (
                            <p className="text-[10px] text-emerald-700 truncate mt-0.5">
                              {item.currentBookingPurpose}
                            </p>
                          )}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Next Booking Info */}
                  {!hasActiveSession && item.nextBookingStartTime && (
                    <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
                      <div className="flex items-center text-slate-500 font-medium">
                        <Calendar className="w-3 h-3 mr-1 text-slate-400" /> Next Confirmed Reservation:
                      </div>
                      <div className="font-semibold text-slate-800">
                        {format(new Date(item.nextBookingStartTime), 'EEE, MMM d • h:mm a')}
                      </div>
                    </div>
                  )}
                </div>

                <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                  <Link
                    to={`/equipment/${item.equipmentId}`}
                    className="text-xs font-semibold text-teal-700 hover:text-teal-800 inline-flex items-center"
                  >
                    View Details & Calendar <ChevronRight className="w-3.5 h-3.5 ml-0.5" />
                  </Link>

                  <span className="text-[10px] text-slate-400">
                    Live updated: {format(lastUpdated, 'HH:mm:ss')}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
