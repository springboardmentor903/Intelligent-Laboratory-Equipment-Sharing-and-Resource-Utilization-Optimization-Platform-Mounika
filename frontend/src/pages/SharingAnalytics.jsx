import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import {
  Share2,
  TrendingUp,
  DollarSign,
  Clock,
  Building,
  Layers,
  FileText,
  AlertCircle,
  CheckCircle2,
  X,
  ArrowUpRight,
  ArrowDownRight,
  PieChart as PieIcon,
  BarChart3,
  Calendar
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';
import { format } from 'date-fns';

export default function SharingAnalytics() {
  const { user } = useAuth();

  const [summary, setSummary] = useState(null);
  const [partners, setPartners] = useState([]);
  const [trends, setTrends] = useState([]);
  const [mostShared, setMostShared] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Partnership Dossier Modal State
  const [selectedPartnerId, setSelectedPartnerId] = useState(null);
  const [partnershipReport, setPartnershipReport] = useState(null);
  const [reportLoading, setReportLoading] = useState(false);

  const fetchAnalytics = () => {
    setLoading(true);
    setError('');

    Promise.all([
      api.get('/sharing/analytics/summary'),
      api.get('/sharing/analytics/partners'),
      api.get('/sharing/analytics/trends?months=6'),
      api.get('/sharing/analytics/most-shared?limit=10'),
    ])
      .then(([sumRes, partRes, trendRes, sharedRes]) => {
        if (sumRes.success) setSummary(sumRes.data);
        if (partRes.success) setPartners(partRes.data);
        if (trendRes.success) setTrends(trendRes.data);
        if (sharedRes.success) setMostShared(sharedRes.data);
      })
      .catch((err) => setError(err.message || 'Failed to load sharing analytics.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const handleOpenReport = (partnerId) => {
    setSelectedPartnerId(partnerId);
    setPartnershipReport(null);
    setReportLoading(true);

    api.get(`/sharing/analytics/partnerships/${partnerId}`)
      .then((res) => {
        if (res.success && res.data) {
          setPartnershipReport(res.data);
        }
      })
      .catch((err) => alert(err.message || 'Failed to load partnership dossier.'))
      .finally(() => setReportLoading(false));
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4 text-center">
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl">
          {error}
        </div>
      </div>
    );
  }

  const isNetPositive = (summary?.netFinancialBalance || 0) >= 0;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Share2 className="w-7 h-7 text-indigo-700" />
            Inter-Institution Sharing Analytics &amp; Reports
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Comprehensive financial reconciliation, bilateral partner balances, and resource exchange metrics.
          </p>
        </div>
      </div>

      {/* Summary KPI Cards */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-1">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">Hours Provided</span>
            <div className="flex items-baseline space-x-1">
              <span className="text-2xl font-extrabold text-teal-700">{summary.hoursProvided}</span>
              <span className="text-xs text-slate-400">hrs</span>
            </div>
            <span className="text-[10px] text-slate-400 block">External usage of your labs</span>
          </div>

          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-1">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">Hours Used</span>
            <div className="flex items-baseline space-x-1">
              <span className="text-2xl font-extrabold text-indigo-700">{summary.hoursUsed}</span>
              <span className="text-xs text-slate-400">hrs</span>
            </div>
            <span className="text-[10px] text-slate-400 block">Consumed at partner labs</span>
          </div>

          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-1">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">Fees Earned</span>
            <div className="text-2xl font-extrabold text-emerald-700">
              ${Number(summary.feesEarned || 0).toFixed(2)}
            </div>
            <span className="text-[10px] text-emerald-600 font-medium block">External revenue</span>
          </div>

          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-1">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">Fees Owed</span>
            <div className="text-2xl font-extrabold text-rose-700">
              ${Number(summary.feesOwed || 0).toFixed(2)}
            </div>
            <span className="text-[10px] text-rose-600 font-medium block">Partner billing owed</span>
          </div>

          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-1">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">Net Balance</span>
            <div className={`text-2xl font-extrabold flex items-center ${isNetPositive ? 'text-emerald-700' : 'text-rose-700'}`}>
              {isNetPositive ? <ArrowUpRight className="w-5 h-5 mr-0.5" /> : <ArrowDownRight className="w-5 h-5 mr-0.5" />}
              ${Math.abs(Number(summary.netFinancialBalance || 0)).toFixed(2)}
            </div>
            <span className="text-[10px] text-slate-400 block">
              {isNetPositive ? 'Net surplus' : 'Net payable'}
            </span>
          </div>

          <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm space-y-1">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">Active Partners</span>
            <div className="text-2xl font-extrabold text-slate-900">
              {summary.activePartnersCount}
            </div>
            <span className="text-[10px] text-slate-400 block">
              {summary.activeAgreementsCount} active agreements
            </span>
          </div>
        </div>
      )}

      {/* Visual Trends Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Hours Provided vs Used */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Clock className="w-4 h-4 text-teal-700" />
                Machine Hours Exchanged (6-Month Trend)
              </h2>
              <p className="text-xs text-slate-500">Comparison of hours provided to partners vs consumed</p>
            </div>
          </div>

          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={trends} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="period" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip
                  formatter={(value, name) => [`${value} hrs`, name === 'hoursProvided' ? 'Provided' : 'Used']}
                  contentStyle={{ backgroundColor: '#fff', borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '11px' }}
                />
                <Legend formatter={(val) => (val === 'hoursProvided' ? 'Hours Provided' : 'Hours Used')} wrapperStyle={{ fontSize: '11px' }} />
                <Bar dataKey="hoursProvided" fill="#0f766e" radius={[4, 4, 0, 0]} />
                <Bar dataKey="hoursUsed" fill="#4f46e5" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Fees Earned vs Owed */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <DollarSign className="w-4 h-4 text-emerald-700" />
                Financial Balance &amp; Fees (6-Month Trend)
              </h2>
              <p className="text-xs text-slate-500">Revenue earned from partners vs usage expenses payable</p>
            </div>
          </div>

          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={trends} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="period" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip
                  formatter={(value, name) => [`$${Number(value).toFixed(2)}`, name === 'feesEarned' ? 'Earned' : 'Owed']}
                  contentStyle={{ backgroundColor: '#fff', borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '11px' }}
                />
                <Legend formatter={(val) => (val === 'feesEarned' ? 'Fees Earned' : 'Fees Owed')} wrapperStyle={{ fontSize: '11px' }} />
                <Bar dataKey="feesEarned" fill="#10b981" radius={[4, 4, 0, 0]} />
                <Bar dataKey="feesOwed" fill="#f43f5e" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Bilateral Partner Institutions Balance Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-200">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Building className="w-5 h-5 text-indigo-700" />
            Bilateral Partner Balances &amp; Exchange Accounts
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Breakdown of instrument hours and financial reciprocity by partner university.
          </p>
        </div>

        {partners.length === 0 ? (
          <div className="text-center py-12 text-slate-400 text-xs">
            No partner institution balances recorded yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs divide-y divide-slate-200">
              <thead className="bg-slate-50 text-slate-700 font-semibold">
                <tr>
                  <th className="p-4">Partner Institution</th>
                  <th className="p-4">Agreement</th>
                  <th className="p-4">Active Grants</th>
                  <th className="p-4">Hours Provided</th>
                  <th className="p-4">Hours Used</th>
                  <th className="p-4">Fees Earned</th>
                  <th className="p-4">Fees Owed</th>
                  <th className="p-4">Net Balance</th>
                  <th className="p-4 text-right">Dossier</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {partners.map((p) => {
                  const net = Number(p.netBalance || 0);
                  const isPositive = net >= 0;

                  return (
                    <tr key={p.partnerInstitutionId} className="hover:bg-slate-50/50">
                      <td className="p-4 font-bold text-slate-900">{p.partnerInstitutionName}</td>
                      <td className="p-4 font-mono text-[11px] text-slate-500">{p.agreementNumber || 'AGR-ACTIVE'}</td>
                      <td className="p-4">{p.activeGrantsCount} credential(s)</td>
                      <td className="p-4 font-medium text-teal-800">{p.hoursProvidedToPartner} hrs</td>
                      <td className="p-4 font-medium text-indigo-800">{p.hoursUsedFromPartner} hrs</td>
                      <td className="p-4 text-emerald-700 font-medium">${Number(p.feesEarnedFromPartner).toFixed(2)}</td>
                      <td className="p-4 text-rose-700 font-medium">${Number(p.feesOwedToPartner).toFixed(2)}</td>
                      <td className="p-4">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full font-bold ${
                            isPositive
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-rose-50 text-rose-700 border border-rose-200'
                          }`}
                        >
                          {isPositive ? `+$${net.toFixed(2)}` : `-$${Math.abs(net).toFixed(2)}`}
                        </span>
                      </td>
                      <td className="p-4 text-right">
                        <button
                          onClick={() => handleOpenReport(p.partnerInstitutionId)}
                          className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold rounded-lg text-xs transition"
                        >
                          View Report
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Most Shared Equipment Ranking */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-200">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Layers className="w-5 h-5 text-teal-700" />
            Top Shared Laboratory Equipment (By External Demand)
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Ranked by external utilization hours and revenue generated from partner institutions.
          </p>
        </div>

        {mostShared.length === 0 ? (
          <div className="text-center py-12 text-slate-400 text-xs">
            No equipment has recorded external usage yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs divide-y divide-slate-200">
              <thead className="bg-slate-50 text-slate-700 font-semibold">
                <tr>
                  <th className="p-4">Equipment Name</th>
                  <th className="p-4">Category</th>
                  <th className="p-4">Department</th>
                  <th className="p-4">External Hours</th>
                  <th className="p-4">Bookings</th>
                  <th className="p-4">External Share %</th>
                  <th className="p-4 text-right">Revenue Generated</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-800">
                {mostShared.map((eq, idx) => (
                  <tr key={eq.equipmentId} className="hover:bg-slate-50/50">
                    <td className="p-4 font-bold text-slate-900 flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-slate-100 text-slate-600 flex items-center justify-center text-[10px] font-bold">
                        {idx + 1}
                      </span>
                      <span>{eq.equipmentName}</span>
                    </td>
                    <td className="p-4">{eq.category}</td>
                    <td className="p-4 text-slate-500">{eq.departmentName}</td>
                    <td className="p-4 font-semibold text-teal-800">{eq.externalHoursUsed} hrs</td>
                    <td className="p-4">{eq.externalBookingsCount} sessions</td>
                    <td className="p-4">
                      <span className="font-semibold text-slate-800">{eq.externalUtilizationRate}%</span> of total
                    </td>
                    <td className="p-4 text-right font-extrabold text-emerald-700">
                      ${Number(eq.revenueGenerated || 0).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Detailed Partnership Report Modal */}
      {selectedPartnerId && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-4xl w-full p-6 shadow-xl border border-slate-200 max-h-[90vh] overflow-y-auto space-y-6">
            <div className="flex justify-between items-center pb-4 border-b border-slate-200">
              <div>
                <h3 className="text-lg font-bold text-slate-900 flex items-center gap-2">
                  <Building className="w-5 h-5 text-indigo-700" />
                  Bilateral Partnership Report &amp; Dossier
                </h3>
                <p className="text-xs text-slate-500">{partnershipReport?.partner?.partnerInstitutionName}</p>
              </div>
              <button
                onClick={() => {
                  setSelectedPartnerId(null);
                  setPartnershipReport(null);
                }}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {reportLoading ? (
              <div className="flex justify-center items-center py-16">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
              </div>
            ) : partnershipReport ? (
              <div className="space-y-6">
                {/* Partner KPI Summary */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3 bg-teal-50 rounded-xl border border-teal-100">
                    <span className="text-[10px] uppercase font-bold text-teal-800 block">Hours Provided</span>
                    <span className="text-xl font-extrabold text-teal-900">
                      {partnershipReport.partner?.hoursProvidedToPartner} hrs
                    </span>
                  </div>
                  <div className="p-3 bg-indigo-50 rounded-xl border border-indigo-100">
                    <span className="text-[10px] uppercase font-bold text-indigo-800 block">Hours Used</span>
                    <span className="text-xl font-extrabold text-indigo-900">
                      {partnershipReport.partner?.hoursUsedFromPartner} hrs
                    </span>
                  </div>
                  <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100">
                    <span className="text-[10px] uppercase font-bold text-emerald-800 block">Fees Earned</span>
                    <span className="text-xl font-extrabold text-emerald-900">
                      ${Number(partnershipReport.partner?.feesEarnedFromPartner || 0).toFixed(2)}
                    </span>
                  </div>
                  <div className="p-3 bg-rose-50 rounded-xl border border-rose-100">
                    <span className="text-[10px] uppercase font-bold text-rose-800 block">Fees Owed</span>
                    <span className="text-xl font-extrabold text-rose-900">
                      ${Number(partnershipReport.partner?.feesOwedToPartner || 0).toFixed(2)}
                    </span>
                  </div>
                </div>

                {/* Equipment Exchanged */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                    <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                      Equipment Provided to Partner
                    </h4>
                    {partnershipReport.equipmentProvided?.length === 0 ? (
                      <p className="text-xs text-slate-400 italic">No equipment provided yet.</p>
                    ) : (
                      <div className="space-y-1.5 text-xs">
                        {partnershipReport.equipmentProvided.map((eq) => (
                          <div key={eq.equipmentId} className="flex justify-between p-2 bg-white rounded-lg border border-slate-100">
                            <span className="font-semibold text-slate-800">{eq.equipmentName}</span>
                            <span className="text-slate-500 font-mono">{eq.externalHoursUsed} hrs • ${Number(eq.revenueGenerated).toFixed(2)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                    <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                      Equipment Consumed from Partner
                    </h4>
                    {partnershipReport.equipmentConsumed?.length === 0 ? (
                      <p className="text-xs text-slate-400 italic">No equipment consumed yet.</p>
                    ) : (
                      <div className="space-y-1.5 text-xs">
                        {partnershipReport.equipmentConsumed.map((eq) => (
                          <div key={eq.equipmentId} className="flex justify-between p-2 bg-white rounded-lg border border-slate-100">
                            <span className="font-semibold text-slate-800">{eq.equipmentName}</span>
                            <span className="text-slate-500 font-mono">{eq.externalHoursUsed} hrs</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* Shared Sessions History */}
                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Recent Shared Sessions Log
                  </h4>
                  {partnershipReport.sharedSessions?.length === 0 ? (
                    <p className="text-xs text-slate-400 italic">No shared sessions logged under this partnership.</p>
                  ) : (
                    <div className="overflow-x-auto border border-slate-200 rounded-xl">
                      <table className="w-full text-left text-xs divide-y divide-slate-200">
                        <thead className="bg-slate-50 text-slate-700 font-semibold">
                          <tr>
                            <th className="p-3">Equipment</th>
                            <th className="p-3">Researcher</th>
                            <th className="p-3">Start Time</th>
                            <th className="p-3">Duration</th>
                            <th className="p-3 text-right">Fee</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 text-slate-800">
                          {partnershipReport.sharedSessions.map((s, idx) => (
                            <tr key={idx}>
                              <td className="p-3 font-semibold">{s.equipmentName}</td>
                              <td className="p-3 text-slate-600">{s.researcherName}</td>
                              <td className="p-3 text-slate-500">
                                {s.sessionStartTime ? format(new Date(s.sessionStartTime), 'MMM d, yyyy h:mm a') : 'N/A'}
                              </td>
                              <td className="p-3 font-mono">{s.durationMinutes} mins</td>
                              <td className="p-3 text-right font-bold text-emerald-700">${s.feeCharged}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              </div>
            ) : null}

            <div className="flex justify-end pt-3 border-t border-slate-200">
              <button
                type="button"
                onClick={() => {
                  setSelectedPartnerId(null);
                  setPartnershipReport(null);
                }}
                className="px-4 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs hover:bg-slate-50 font-medium"
              >
                Close Dossier
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
