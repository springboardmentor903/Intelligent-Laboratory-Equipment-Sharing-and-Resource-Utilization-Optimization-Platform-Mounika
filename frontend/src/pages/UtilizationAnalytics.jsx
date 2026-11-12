import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area
} from 'recharts';
import { scaleLinear } from 'd3-scale';
import {
  TrendingUp,
  BarChart3,
  Calendar,
  Clock,
  AlertTriangle,
  Flame,
  CheckCircle2,
  RefreshCw,
  Info,
  ChevronRight,
  Filter
} from 'lucide-react';
import { format, subDays, startOfDay, endOfDay } from 'date-fns';

export default function UtilizationAnalytics() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Scope parameters from URL or smart defaults
  const scopeType = searchParams.get('scopeType') || (user?.department?.id ? 'DEPARTMENT' : user?.institution?.id ? 'INSTITUTION' : 'INSTITUTION');
  const scopeId = searchParams.get('scopeId') || (scopeType === 'DEPARTMENT' ? user?.department?.id : user?.institution?.id) || '1';
  const period = searchParams.get('period') || 'LAST_30_DAYS';

  // Compute dates based on period
  const { startDateStr, endDateStr } = useMemo(() => {
    const today = new Date();
    if (period === 'TODAY') {
      const s = format(today, 'yyyy-MM-dd');
      return { startDateStr: s, endDateStr: s };
    } else if (period === 'LAST_7_DAYS') {
      return {
        startDateStr: format(subDays(today, 6), 'yyyy-MM-dd'),
        endDateStr: format(today, 'yyyy-MM-dd'),
      };
    } else {
      // LAST_30_DAYS default
      return {
        startDateStr: format(subDays(today, 29), 'yyyy-MM-dd'),
        endDateStr: format(today, 'yyyy-MM-dd'),
      };
    }
  }, [period]);

  // Section data states
  const [summary, setSummary] = useState(null);
  const [loadingSummary, setLoadingSummary] = useState(true);
  const [summaryError, setSummaryError] = useState('');

  const [benchmark, setBenchmark] = useState(null);
  const [loadingBenchmark, setLoadingBenchmark] = useState(true);

  const [heatmapData, setHeatmapData] = useState(null);
  const [loadingHeatmap, setLoadingHeatmap] = useState(true);
  const [hoveredCell, setHoveredCell] = useState(null);

  const [peakUsage, setPeakUsage] = useState(null);
  const [loadingPeak, setLoadingPeak] = useState(true);

  const [idleData, setIdleData] = useState(null);
  const [idleAlerts, setIdleAlerts] = useState([]);
  const [loadingIdle, setLoadingIdle] = useState(true);

  const [demandList, setDemandList] = useState([]);
  const [highDemandOnly, setHighDemandOnly] = useState(false);
  const [loadingDemand, setLoadingDemand] = useState(true);

  // Scope options for selector
  const [institutions, setInstitutions] = useState([]);
  const [departments, setDepartments] = useState([]);

  useEffect(() => {
    api.get('/institutions').then((res) => {
      if (res.success && res.data) setInstitutions(res.data);
    });
    api.get('/institutions/departments').then((res) => {
      if (res.success && res.data) setDepartments(res.data);
    });
  }, []);

  const updateFilters = (newParams) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(newParams).forEach(([k, v]) => {
      if (v != null) next.set(k, v);
      else next.delete(k);
    });
    setSearchParams(next);
  };

  // 1. Fetch Summary
  const fetchSummary = () => {
    setLoadingSummary(true);
    api.get('/analytics/utilization', {
      params: { scopeType, scopeId, startDate: startDateStr, endDate: endDateStr }
    })
      .then((res) => {
        if (res.success) setSummary(res.data);
      })
      .catch((err) => setSummaryError(err.message || 'Failed to load summary.'))
      .finally(() => setLoadingSummary(false));
  };

  // 2. Fetch Benchmark
  const fetchBenchmark = () => {
    setLoadingBenchmark(true);
    api.get('/analytics/benchmark', {
      params: { scopeType, scopeId, startDate: startDateStr, endDate: endDateStr }
    })
      .then((res) => {
        if (res.success) setBenchmark(res.data);
      })
      .catch(() => {})
      .finally(() => setLoadingBenchmark(false));
  };

  // 3. Fetch Heatmap
  const fetchHeatmap = () => {
    setLoadingHeatmap(true);
    api.get('/analytics/heatmap', {
      params: { scopeType, scopeId, startDate: startDateStr, endDate: endDateStr }
    })
      .then((res) => {
        if (res.success) setHeatmapData(res.data);
      })
      .catch(() => {})
      .finally(() => setLoadingHeatmap(false));
  };

  // 4. Fetch Peak Usage
  const fetchPeak = () => {
    setLoadingPeak(true);
    api.get('/analytics/peak-usage', {
      params: { scopeType, scopeId, startDate: startDateStr, endDate: endDateStr }
    })
      .then((res) => {
        if (res.success) setPeakUsage(res.data);
      })
      .catch(() => {})
      .finally(() => setLoadingPeak(false));
  };

  // 5. Fetch Idle Analysis & Alerts
  const fetchIdle = () => {
    setLoadingIdle(true);
    Promise.all([
      api.get('/analytics/idle-time', {
        params: { scopeType, scopeId, startDate: startDateStr, endDate: endDateStr }
      }),
      api.get('/analytics/idle-alerts')
    ])
      .then(([idleRes, alertsRes]) => {
        if (idleRes.success) setIdleData(idleRes.data);
        if (alertsRes.success) setIdleAlerts(alertsRes.data);
      })
      .catch(() => {})
      .finally(() => setLoadingIdle(false));
  };

  // 6. Fetch Demand Analysis
  const fetchDemand = () => {
    setLoadingDemand(true);
    api.get('/analytics/demand', {
      params: { scopeType, scopeId, startDate: startDateStr, endDate: endDateStr, highDemandOnly }
    })
      .then((res) => {
        if (res.success) setDemandList(res.data);
      })
      .catch(() => {})
      .finally(() => setLoadingDemand(false));
  };

  useEffect(() => {
    fetchSummary();
    fetchBenchmark();
    fetchHeatmap();
    fetchPeak();
    fetchIdle();
    fetchDemand();
  }, [scopeType, scopeId, startDateStr, endDateStr, highDemandOnly]);

  // Heatmap D3 color scale (emerald green scale)
  const colorScale = useMemo(() => {
    return scaleLinear()
      .domain([0, 25, 50, 75, 100])
      .range(['#ecfdf5', '#a7f3d0', '#34d399', '#059669', '#064e3b']);
  }, []);

  const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-8">
      {/* Header & Controls */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2.5">
            <BarChart3 className="w-8 h-8 text-teal-700" />
            Utilization Analytics &amp; Optimization
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Real capacity analysis, benchmark comparisons, idle alerts, and 168-hour utilization heatmaps.
          </p>
        </div>

        {/* Scope and Period Pickers */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Scope Selector */}
          <select
            value={`${scopeType}:${scopeId}`}
            onChange={(e) => {
              const [st, sid] = e.target.value.split(':');
              updateFilters({ scopeType: st, scopeId: sid });
            }}
            className="px-3 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 bg-white focus:ring-2 focus:ring-teal-600 focus:outline-none"
          >
            <optgroup label="Institutions">
              {institutions.map((i) => (
                <option key={`INSTITUTION:${i.id}`} value={`INSTITUTION:${i.id}`}>
                  Institution: {i.name}
                </option>
              ))}
            </optgroup>
            <optgroup label="Departments">
              {departments.map((d) => (
                <option key={`DEPARTMENT:${d.id}`} value={`DEPARTMENT:${d.id}`}>
                  Dept: {d.name} ({d.code})
                </option>
              ))}
            </optgroup>
          </select>

          {/* Period Selector */}
          <div className="inline-flex rounded-xl p-1 bg-slate-200/80 text-xs font-semibold">
            <button
              onClick={() => updateFilters({ period: 'TODAY' })}
              className={`px-3 py-1.5 rounded-lg transition ${
                period === 'TODAY' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Today
            </button>
            <button
              onClick={() => updateFilters({ period: 'LAST_7_DAYS' })}
              className={`px-3 py-1.5 rounded-lg transition ${
                period === 'LAST_7_DAYS' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Last 7 Days
            </button>
            <button
              onClick={() => updateFilters({ period: 'LAST_30_DAYS' })}
              className={`px-3 py-1.5 rounded-lg transition ${
                period === 'LAST_30_DAYS' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Last 30 Days
            </button>
          </div>
        </div>
      </div>

      {/* Utilization Summary & Benchmark Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* KPI 1: Utilization Rate */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-2">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
            <span>Utilization Rate</span>
            <span className="text-[10px] font-mono uppercase bg-slate-100 px-2 py-0.5 rounded text-slate-600">
              Operating Hours
            </span>
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {summary?.utilizationRate != null ? `${summary.utilizationRate}%` : '0.00%'}
            </span>
            {summary?.targetStatus && (
              <span
                className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                  summary.targetStatus === 'EXCEEDED'
                    ? 'bg-emerald-100 text-emerald-800'
                    : summary.targetStatus === 'ON_TRACK'
                    ? 'bg-teal-100 text-teal-800'
                    : 'bg-amber-100 text-amber-800'
                }`}
              >
                {summary.targetStatus.replace('_', ' ')}
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500">
            Target: {summary?.targetPercentage || '70.00'}% • Gap:{' '}
            <span className={Number(summary?.targetGap || 0) >= 0 ? 'text-emerald-600 font-bold' : 'text-amber-600 font-bold'}>
              {summary?.targetGap != null ? `${Number(summary.targetGap) > 0 ? '+' : ''}${summary.targetGap}%` : '0%'}
            </span>
          </p>
        </div>

        {/* KPI 2: Hours Used vs Available */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-2">
          <span className="text-xs font-semibold text-slate-500">Used vs Capacity</span>
          <div className="flex items-baseline space-x-1">
            <span className="text-3xl font-extrabold text-teal-700">
              {summary?.totalUsedHours != null ? `${summary.totalUsedHours}h` : '0h'}
            </span>
            <span className="text-slate-400 font-medium text-sm">
              / {summary?.totalAvailableHours != null ? `${summary.totalAvailableHours}h` : '0h'} avail
            </span>
          </div>
          <p className="text-xs text-slate-500">
            Total active usage logs within verified operating schedule.
          </p>
        </div>

        {/* KPI 3: Shared Usage % */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-2">
          <span className="text-xs font-semibold text-slate-500">Inter-Institution Shared</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-indigo-700">
              {summary?.sharedUsagePercentage != null ? `${summary.sharedUsagePercentage}%` : '0.00%'}
            </span>
          </div>
          <p className="text-xs text-slate-500">
            {summary?.sharedUsedMinutes ? Math.round(summary.sharedUsedMinutes / 60) : 0}h provided to partner institutions.
          </p>
        </div>

        {/* KPI 4: 28-Day Benchmark */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-2">
          <span className="text-xs font-semibold text-slate-500">28-Day Historical Benchmark</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-slate-900">
              {benchmark?.benchmarkUtilizationRate != null ? `${benchmark.benchmarkUtilizationRate}%` : '0.00%'}
            </span>
            {benchmark?.comparisonStatus && (
              <span
                className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                  benchmark.comparisonStatus === 'INCREASED'
                    ? 'bg-emerald-100 text-emerald-800'
                    : benchmark.comparisonStatus === 'DECREASED'
                    ? 'bg-rose-100 text-rose-800'
                    : 'bg-slate-100 text-slate-700'
                }`}
              >
                {benchmark.differencePercentagePoints > 0 ? '+' : ''}
                {benchmark.differencePercentagePoints}%
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500">
            Trend comparison vs previous 28-day baseline period.
          </p>
        </div>
      </div>

      {/* Heatmap Section */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-3 border-b border-slate-100 gap-2">
          <div>
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Flame className="w-5 h-5 text-amber-500" />
              168-Hour Weekly Utilization Heatmap
            </h2>
            <p className="text-xs text-slate-500">
              Visualizes demand intensity across every hour of the week (Platform Timezone: {heatmapData?.platformTimezone || 'UTC'}).
            </p>
          </div>

          {/* Heatmap Color Legend */}
          <div className="flex items-center space-x-2 text-[11px] text-slate-600">
            <span className="flex items-center gap-1">
              <span className="w-3.5 h-3.5 bg-slate-200 rounded border border-slate-300"></span> Closed
            </span>
            <span className="flex items-center gap-1">
              <span className="w-3.5 h-3.5 bg-emerald-50 rounded border border-emerald-200"></span> 0%
            </span>
            <span className="flex items-center gap-1">
              <span className="w-3.5 h-3.5 bg-emerald-300 rounded"></span> 50%
            </span>
            <span className="flex items-center gap-1">
              <span className="w-3.5 h-3.5 bg-emerald-700 rounded"></span> 100%
            </span>
          </div>
        </div>

        {/* Heatmap Grid */}
        {loadingHeatmap ? (
          <div className="flex justify-center items-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600"></div>
          </div>
        ) : (
          <div className="overflow-x-auto pb-2">
            <div className="min-w-[760px]">
              {/* Hour Column Labels (00 to 23) */}
              <div className="grid grid-cols-[48px_repeat(24,1fr)] gap-1 text-[10px] font-mono text-slate-400 mb-1 text-center">
                <div></div>
                {Array.from({ length: 24 }).map((_, h) => (
                  <div key={h}>{h}</div>
                ))}
              </div>

              {/* Day Rows */}
              {dayLabels.map((dayName, dayIdx) => {
                const dayNumber = dayIdx + 1;
                const daySlots = (heatmapData?.slots || []).filter((s) => s.dayOfWeek === dayNumber);

                return (
                  <div key={dayNumber} className="grid grid-cols-[48px_repeat(24,1fr)] gap-1 mb-1 items-center">
                    <span className="text-xs font-bold text-slate-600">{dayName}</span>
                    {Array.from({ length: 24 }).map((_, hour) => {
                      const slot = daySlots.find((s) => s.hourOfDay === hour);
                      const isClosed = !slot || !slot.open;
                      const pct = slot?.utilizationPercentage != null ? Number(slot.utilizationPercentage) : null;

                      let bgColor = '#e2e8f0'; // closed grey
                      if (!isClosed) {
                        bgColor = pct === 0 ? '#ecfdf5' : colorScale(pct);
                      }

                      return (
                        <div
                          key={hour}
                          onMouseEnter={() => setHoveredCell(slot)}
                          onMouseLeave={() => setHoveredCell(null)}
                          style={{ backgroundColor: bgColor }}
                          className="h-6 rounded cursor-pointer transition hover:scale-110 hover:ring-2 hover:ring-teal-700 relative"
                        />
                      );
                    })}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Tooltip Card for hovered slot */}
        {hoveredCell && (
          <div className="p-3 bg-slate-900 text-white rounded-xl text-xs flex items-center justify-between shadow-lg">
            <div>
              <span className="font-bold text-teal-300">{hoveredCell.dayName}</span> at{' '}
              <span className="font-mono">{String(hoveredCell.hourOfDay).padStart(2, '0')}:00 – {String(hoveredCell.hourOfDay + 1).padStart(2, '0')}:00</span>
            </div>
            <div className="flex items-center space-x-4 text-slate-300">
              <span>Status: <strong className={hoveredCell.open ? 'text-emerald-400' : 'text-slate-400'}>{hoveredCell.open ? 'OPEN' : 'CLOSED'}</strong></span>
              <span>Used: <strong>{hoveredCell.usedMinutes} mins</strong></span>
              <span>Capacity: <strong>{hoveredCell.availableMinutes} mins</strong></span>
              <span className="text-white font-bold bg-teal-800 px-2 py-0.5 rounded">
                {hoveredCell.utilizationPercentage != null ? `${hoveredCell.utilizationPercentage}%` : 'N/A (Closed)'}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Peak Usage Analysis & Idle Alert Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Peak Usage Section */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Clock className="w-5 h-5 text-indigo-600" />
                Peak Operating Patterns
              </h2>
              <p className="text-xs text-slate-500">Usage breakdown across weekdays and operating hours.</p>
            </div>
            <div className="text-right">
              <span className="text-xs text-slate-400">Busiest Day:</span>
              <p className="text-xs font-bold text-indigo-700">{peakUsage?.busiestDayName || 'N/A'}</p>
            </div>
          </div>

          {loadingPeak ? (
            <div className="flex justify-center items-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600"></div>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="h-44">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={dayLabels.map((name, i) => ({
                      name,
                      minutes: peakUsage?.dayOfWeekUsageMinutes?.[i + 1] || 0
                    }))}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                    <XAxis dataKey="name" fontSize={11} stroke="#64748b" />
                    <YAxis fontSize={11} stroke="#64748b" />
                    <Tooltip formatter={(value) => [`${value} mins`, 'Usage Time']} />
                    <Bar dataKey="minutes" fill="#0d9488" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>

              {/* Top 5 Peak Slots List */}
              <div className="pt-2 border-t border-slate-100">
                <h4 className="text-xs font-bold text-slate-700 mb-2">Top 5 Busiest Day-and-Hour Windows:</h4>
                <div className="space-y-1.5">
                  {(peakUsage?.top5BusiestSlots || []).map((slot, idx) => (
                    <div key={idx} className="flex items-center justify-between text-xs p-2 bg-slate-50 rounded-lg">
                      <span className="font-semibold text-slate-800">
                        #{idx + 1} {slot.dayName} • {slot.hourOfDay}:00 – {slot.hourOfDay + 1}:00
                      </span>
                      <span className="font-mono font-bold text-teal-700">
                        {slot.usedMinutes} mins logged
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Idle Time & High Idle Alerts Section */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-amber-500" />
                  Idle Capacity &amp; Underutilization Alerts
                </h2>
                <p className="text-xs text-slate-500">Instruments with ≥ 90% idle time over the last 7 days.</p>
              </div>
              <span className="text-xs font-bold px-2 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200">
                {idleAlerts.length} Active Alerts
              </span>
            </div>

            {loadingIdle ? (
              <div className="flex justify-center items-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600"></div>
              </div>
            ) : idleAlerts.length === 0 ? (
              <div className="text-center py-12 bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
                <p className="text-xs font-semibold text-slate-700">No Underutilization Alerts</p>
                <p className="text-[11px] text-slate-500 mt-0.5">All bookable machines are operating above the 10% minimum threshold.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-100 overflow-y-auto max-h-72">
                {idleAlerts.map((alert) => (
                  <div key={alert.equipmentId} className="py-2.5 flex items-center justify-between">
                    <div>
                      <Link
                        to={`/equipment/${alert.equipmentId}`}
                        className="text-xs font-bold text-slate-900 hover:text-teal-700 transition"
                      >
                        {alert.equipmentName}
                      </Link>
                      <p className="text-[11px] text-slate-500">
                        Tag: {alert.assetTag} • {alert.departmentName}
                      </p>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded">
                        {alert.idlePercentage}% Idle
                      </span>
                      <p className="text-[10px] text-slate-400 mt-0.5">
                        {alert.idleHours}h unoccupied
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-3 bg-teal-50/70 border border-teal-200 rounded-xl text-xs text-teal-900 flex items-center justify-between">
            <span>Opportunity: Consider listing high-idle machines on the inter-institution sharing catalog.</span>
            <Link to="/shared-equipment" className="font-bold underline text-teal-800 ml-2">
              Share Resources
            </Link>
          </div>
        </div>
      </div>

      {/* Demand Analysis Table */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-3 border-b border-slate-100 gap-3">
          <div>
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-teal-700" />
              Equipment Demand &amp; Booking Behavior Analysis
            </h2>
            <p className="text-xs text-slate-500">
              Aggregated booking rates, no-show rates, cancellations, and high-demand classification (threshold: {summary?.targetPercentage ? '80%' : '80%'}).
            </p>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setHighDemandOnly(!highDemandOnly)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition ${
                highDemandOnly
                  ? 'bg-rose-50 border-rose-300 text-rose-700'
                  : 'bg-white border-slate-300 text-slate-600 hover:bg-slate-50'
              }`}
            >
              {highDemandOnly ? 'Showing High Demand Only' : 'Filter High Demand (≥80%)'}
            </button>
          </div>
        </div>

        {loadingDemand ? (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600"></div>
          </div>
        ) : demandList.length === 0 ? (
          <div className="text-center py-12 text-slate-500 text-xs">
            No equipment records found for this scope.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Equipment / Asset Tag</th>
                  <th className="py-3 px-4">Department</th>
                  <th className="py-3 px-4 text-right">Bookings</th>
                  <th className="py-3 px-4 text-right">Booking Rate</th>
                  <th className="py-3 px-4 text-right">No-Show Rate</th>
                  <th className="py-3 px-4 text-right">Cancellation Rate</th>
                  <th className="py-3 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {demandList.map((item) => (
                  <tr key={item.equipmentId} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 font-semibold text-slate-900">
                      <Link to={`/equipment/${item.equipmentId}`} className="hover:text-teal-700">
                        {item.equipmentName}
                      </Link>
                      <span className="block text-[11px] font-mono text-slate-400 font-normal">
                        {item.assetTag}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-600">{item.departmentName}</td>
                    <td className="py-3 px-4 text-right font-mono font-medium">{item.totalBookings}</td>
                    <td className="py-3 px-4 text-right font-mono font-bold text-slate-900">
                      {item.bookingRate}%
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-amber-700">
                      {item.noShowRate}%
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-slate-500">
                      {item.cancellationRate}%
                    </td>
                    <td className="py-3 px-4 text-center">
                      {item.highDemand ? (
                        <span className="px-2 py-0.5 bg-rose-100 text-rose-800 rounded-full text-[10px] font-bold">
                          HIGH DEMAND
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded-full text-[10px] font-medium">
                          NORMAL
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
