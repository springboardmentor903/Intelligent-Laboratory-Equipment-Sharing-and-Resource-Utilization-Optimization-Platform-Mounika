import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { canManageSharing, isInstitutionAdmin, isSystemAdmin } from '../utils/roleHelper';
import {
  FileText,
  Building,
  Calendar,
  DollarSign,
  AlertCircle,
  CheckCircle2,
  X,
  Shield,
  Layers,
  Receipt,
  Clock,
  User
} from 'lucide-react';
import { format } from 'date-fns';

export default function Agreements() {
  const { user } = useAuth();

  const [agreements, setAgreements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Revoke Dialog State
  const [revokeDialog, setRevokeDialog] = useState({ open: false, agreementId: null, reason: '' });
  const [revoking, setRevoking] = useState(false);

  // Itemized Fees Modal State
  const [feeModal, setFeeModal] = useState({ open: false, agreement: null, fees: [], loading: false });

  const fetchAgreements = () => {
    setLoading(true);
    api.get('/sharing/agreements')
      .then((res) => {
        if (res.success && res.data) {
          setAgreements(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load sharing agreements.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAgreements();
  }, []);

  const handleOpenFees = (agr) => {
    setFeeModal({ open: true, agreement: agr, fees: [], loading: true });
    api.get(`/sharing/agreements/${agr.id}/fees`)
      .then((res) => {
        if (res.success && res.data) {
          setFeeModal((prev) => ({ ...prev, fees: res.data, loading: false }));
        }
      })
      .catch((err) => {
        alert(err.message || 'Failed to load itemized usage fees.');
        setFeeModal((prev) => ({ ...prev, loading: false }));
      });
  };

  const handleRevoke = async (e) => {
    e.preventDefault();
    setRevoking(true);
    try {
      const res = await api.put(`/sharing/agreements/${revokeDialog.agreementId}/revoke`, {
        reason: revokeDialog.reason || 'Revoked by institution administrator.',
      });
      if (res.success) {
        setActionSuccess('Sharing agreement and all associated user access grants have been revoked.');
        setRevokeDialog({ open: false, agreementId: null, reason: '' });
        fetchAgreements();
        setTimeout(() => setActionSuccess(''), 3500);
      }
    } catch (err) {
      alert(err.message || 'Failed to revoke agreement.');
    } finally {
      setRevoking(false);
    }
  };

  const isIA = canManageSharing(user);

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <FileText className="w-7 h-7 text-indigo-700" />
            Bilateral Resource Sharing Agreements
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Formal institutional agreements governing cross-laboratory instrument utilization, fee structures, and liability.
          </p>
        </div>

        <div className="text-xs font-semibold bg-indigo-50 text-indigo-800 border border-indigo-200 px-3 py-1.5 rounded-xl">
          Active Agreements: {agreements.filter((a) => a.status === 'ACTIVE').length}
        </div>
      </div>

      {actionSuccess && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          <span>{actionSuccess}</span>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
        </div>
      ) : error ? (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-center text-sm">
          {error}
        </div>
      ) : agreements.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <FileText className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">No Sharing Agreements Found</h3>
          <p className="text-sm text-slate-500 mt-1">
            Bilateral agreements are automatically established when host institutions approve external access requests.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden divide-y divide-slate-200">
          {agreements.map((agr) => {
            const userInstId = user?.institution?.id || user?.department?.institution?.id;
            const isCallerProvider = userInstId && userInstId === agr.providerInstitution?.id;
            const isCallerConsumer = userInstId && userInstId === agr.consumerInstitution?.id;

            return (
              <div key={agr.id} className="p-6 hover:bg-slate-50/50 transition flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                <div className="space-y-3 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-bold text-indigo-900 bg-indigo-50 border border-indigo-200 px-2.5 py-1 rounded-md">
                      {agr.agreementNumber}
                    </span>
                    <span
                      className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${
                        agr.status === 'ACTIVE'
                          ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          : 'bg-rose-50 text-rose-800 border border-rose-200'
                      }`}
                    >
                      {agr.status}
                    </span>
                    <span className="text-xs text-slate-500">
                      Rate: <strong className="text-slate-800">${agr.feePerHour} / hr</strong>
                    </span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs bg-slate-50 p-3 rounded-xl border border-slate-200/80 max-w-2xl">
                    <div>
                      <span className="text-slate-400 block font-medium">Provider (Equipment Host):</span>
                      <span className="font-bold text-slate-900 flex items-center gap-1 mt-0.5">
                        <Building className="w-3.5 h-3.5 text-teal-600" />
                        {agr.providerInstitution?.name}
                        {isCallerProvider && (
                          <span className="text-[10px] bg-teal-100 text-teal-800 px-1.5 py-0.5 rounded font-normal">Your Institution</span>
                        )}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block font-medium">Consumer (Visiting Institution):</span>
                      <span className="font-bold text-slate-900 flex items-center gap-1 mt-0.5">
                        <Building className="w-3.5 h-3.5 text-indigo-600" />
                        {agr.consumerInstitution?.name}
                        {isCallerConsumer && (
                          <span className="text-[10px] bg-indigo-100 text-indigo-800 px-1.5 py-0.5 rounded font-normal">Your Institution</span>
                        )}
                      </span>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                    <div className="flex items-center">
                      <Calendar className="w-3.5 h-3.5 mr-1 text-slate-400" />
                      <span>Valid Window: <strong>{agr.validFrom}</strong> to <strong>{agr.validTo}</strong></span>
                    </div>
                  </div>

                  {agr.revocationReason && (
                    <div className="text-[11px] p-2 bg-rose-50 border border-rose-100 rounded-lg text-rose-900 max-w-xl">
                      <strong>Revocation Note:</strong> {agr.revocationReason}
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="flex items-center space-x-2 flex-shrink-0">
                  <button
                    onClick={() => handleOpenFees(agr)}
                    className="px-3.5 py-2 border border-slate-300 hover:bg-slate-100 text-slate-700 rounded-xl text-xs font-semibold transition flex items-center gap-1"
                  >
                    <Receipt className="w-3.5 h-3.5 text-slate-500" />
                    <span>Itemized Fees</span>
                  </button>

                  {isIA && agr.status === 'ACTIVE' && (
                    <button
                      onClick={() => setRevokeDialog({ open: true, agreementId: agr.id, reason: '' })}
                      className="px-3.5 py-2 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-semibold transition"
                    >
                      Revoke
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Itemized Fees Modal */}
      {feeModal.open && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-3xl w-full p-6 shadow-xl border border-slate-200 max-h-[85vh] overflow-y-auto">
            <div className="flex justify-between items-center pb-3 border-b border-slate-200">
              <div>
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <Receipt className="w-4 h-4 text-indigo-700" />
                  Itemized Usage Fees Breakdown
                </h3>
                <p className="text-xs text-slate-500">Agreement {feeModal.agreement?.agreementNumber}</p>
              </div>
              <button
                onClick={() => setFeeModal({ open: false, agreement: null, fees: [], loading: false })}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="my-4">
              {feeModal.loading ? (
                <div className="flex justify-center items-center py-10">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                </div>
              ) : feeModal.fees.length === 0 ? (
                <div className="text-center py-10 text-slate-500 text-xs">
                  No completed sessions with calculated fees recorded under this agreement yet.
                </div>
              ) : (
                <div className="overflow-x-auto border border-slate-200 rounded-xl">
                  <table className="w-full text-left text-xs divide-y divide-slate-200">
                    <thead className="bg-slate-50 text-slate-700 font-semibold">
                      <tr>
                        <th className="p-3">Equipment</th>
                        <th className="p-3">Researcher</th>
                        <th className="p-3">Session Start</th>
                        <th className="p-3">Duration</th>
                        <th className="p-3">Rate</th>
                        <th className="p-3 text-right">Fee Charged</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-slate-800">
                      {feeModal.fees.map((fee, idx) => (
                        <tr key={idx} className="hover:bg-slate-50">
                          <td className="p-3 font-semibold">{fee.equipmentName}</td>
                          <td className="p-3 text-slate-600">{fee.researcherName}</td>
                          <td className="p-3 text-slate-500">
                            {fee.sessionStartTime ? format(new Date(fee.sessionStartTime), 'MMM d, yyyy h:mm a') : 'N/A'}
                          </td>
                          <td className="p-3 font-mono">{fee.durationMinutes} mins</td>
                          <td className="p-3 text-slate-600">${fee.hourlyRate}/hr</td>
                          <td className="p-3 text-right font-bold text-teal-700">${fee.feeCharged}</td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot className="bg-slate-50 font-bold">
                      <tr>
                        <td colSpan={5} className="p-3 text-slate-700 text-right">Total Agreement Billing:</td>
                        <td className="p-3 text-right text-teal-800">
                          $
                          {feeModal.fees
                            .reduce((acc, f) => acc + Number(f.feeCharged || 0), 0)
                            .toFixed(2)}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-2 border-t border-slate-200">
              <button
                type="button"
                onClick={() => setFeeModal({ open: false, agreement: null, fees: [], loading: false })}
                className="px-4 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs hover:bg-slate-50"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Revoke Dialog */}
      {revokeDialog.open && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 mb-2">Revoke Bilateral Agreement</h3>
            <p className="text-xs text-slate-600 mb-4">
              Revoking this agreement will immediately terminate all active Access Grants issued to visiting researchers under this partnership.
            </p>

            <form onSubmit={handleRevoke} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Revocation Justification / Reason *
                </label>
                <textarea
                  rows={3}
                  required
                  value={revokeDialog.reason}
                  onChange={(e) => setRevokeDialog({ ...revokeDialog, reason: e.target.value })}
                  placeholder="e.g. Partnership term completed or facility decommissioned."
                  className="w-full p-2.5 border border-slate-300 rounded-xl text-xs"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRevokeDialog({ open: false, agreementId: null, reason: '' })}
                  className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={revoking}
                  className="px-4 py-1.5 bg-rose-600 text-white rounded-xl text-xs font-semibold hover:bg-rose-700 disabled:opacity-50"
                >
                  {revoking ? 'Revoking...' : 'Confirm Revocation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
