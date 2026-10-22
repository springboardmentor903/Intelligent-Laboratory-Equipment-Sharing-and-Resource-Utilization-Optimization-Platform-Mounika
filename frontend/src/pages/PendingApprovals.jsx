import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { BookingStatusBadge } from '../components/common/StatusBadge';
import { CheckSquare, Calendar, Clock, User, Check, X, AlertCircle, CheckCircle2, Beaker } from 'lucide-react';
import { format } from 'date-fns';

export default function PendingApprovals() {
  const [pendingList, setPendingList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Reject Modal State
  const [rejectId, setRejectId] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [submittingAction, setSubmittingAction] = useState(false);

  const fetchPending = () => {
    setLoading(true);
    api.get('/bookings/pending')
      .then((res) => {
        if (res.success && res.data) {
          setPendingList(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load pending approvals queue.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchPending();
  }, []);

  const handleApprove = async (id) => {
    setSubmittingAction(true);
    try {
      const res = await api.put(`/bookings/${id}/approval`, {
        approved: true,
        reason: 'Approved by Lab Manager',
      });
      if (res.success) {
        setActionSuccess(`Reservation #${id} approved successfully!`);
        fetchPending();
        setTimeout(() => setActionSuccess(''), 3000);
      }
    } catch (err) {
      alert(err.message || 'Failed to approve booking.');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleReject = async (e) => {
    e.preventDefault();
    if (!rejectId) return;

    setSubmittingAction(true);
    try {
      const res = await api.put(`/bookings/${rejectId}/approval`, {
        approved: false,
        reason: rejectReason || 'Schedule conflict or maintenance required.',
      });
      if (res.success) {
        setActionSuccess(`Reservation #${rejectId} rejected.`);
        setRejectId(null);
        setRejectReason('');
        fetchPending();
        setTimeout(() => setActionSuccess(''), 3000);
      }
    } catch (err) {
      alert(err.message || 'Failed to reject booking.');
    } finally {
      setSubmittingAction(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Booking Approvals Queue
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Review and manage reservation requests submitted by researchers and faculty members.
          </p>
        </div>

        <div className="text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200 px-3 py-1.5 rounded-xl">
          Pending Reviews: {pendingList.length}
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
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-600"></div>
        </div>
      ) : error ? (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-center text-sm">
          {error}
        </div>
      ) : pendingList.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <CheckSquare className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">Approvals Queue is Clear</h3>
          <p className="text-sm text-slate-500 mt-1">There are no pending equipment reservations awaiting review.</p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden divide-y divide-slate-200">
          {pendingList.map((item) => {
            const start = new Date(item.startTime);
            const end = new Date(item.endTime);

            return (
              <div key={item.id} className="p-6 hover:bg-slate-50 transition flex flex-col lg:flex-row lg:items-center justify-between gap-6">
                <div className="space-y-3 flex-1">
                  <div className="flex items-center space-x-3">
                    <span className="text-base font-bold text-slate-900">{item.equipment.name}</span>
                    <BookingStatusBadge status={item.status} />
                  </div>

                  {/* Researcher info */}
                  <div className="flex items-center space-x-2 text-xs text-slate-700">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-semibold">{item.user.fullName}</span>
                    <span className="text-slate-400">•</span>
                    <span>{item.user.email}</span>
                    <span className="text-slate-400">•</span>
                    <span>{item.user.department?.name || 'Department'}</span>
                  </div>

                  {/* Time Window */}
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-100 max-w-xl">
                    <div className="flex items-center">
                      <Calendar className="w-3.5 h-3.5 mr-1 text-teal-600" />
                      <span className="font-medium">{format(start, 'EEEE, MMMM d, yyyy')}</span>
                    </div>
                    <div className="flex items-center">
                      <Clock className="w-3.5 h-3.5 mr-1 text-teal-600" />
                      <span className="font-semibold text-slate-800">{format(start, 'h:mm a')} – {format(end, 'h:mm a')}</span>
                    </div>
                  </div>

                  {/* Purpose */}
                  <p className="text-xs text-slate-700 max-w-2xl leading-relaxed">
                    <span className="font-semibold text-slate-800">Research Purpose:</span> {item.purpose}
                  </p>

                  {item.notes && (
                    <p className="text-xs text-slate-500 italic">
                      Special requests: {item.notes}
                    </p>
                  )}
                </div>

                {/* Approve / Reject Actions */}
                <div className="flex items-center space-x-3 flex-shrink-0">
                  <button
                    onClick={() => handleApprove(item.id)}
                    disabled={submittingAction}
                    className="flex items-center space-x-1.5 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold shadow-sm transition disabled:opacity-50"
                  >
                    <Check className="w-4 h-4" />
                    <span>Approve</span>
                  </button>

                  <button
                    onClick={() => {
                      setRejectId(item.id);
                      setRejectReason('');
                    }}
                    disabled={submittingAction}
                    className="flex items-center space-x-1.5 px-4 py-2 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-semibold transition disabled:opacity-50"
                  >
                    <X className="w-4 h-4" />
                    <span>Reject</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Rejection Modal */}
      {rejectId && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <h3 className="text-lg font-bold text-slate-900 mb-2">Reject Reservation Request</h3>
            <p className="text-xs text-slate-600 mb-4">
              Please provide a reason for declining this equipment reservation. The researcher will be notified with this feedback.
            </p>

            <form onSubmit={handleReject} className="space-y-4">
              <textarea
                rows={3}
                required
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="e.g. Scheduled emergency laser calibration; please pick an afternoon slot."
                className="w-full p-2.5 border border-slate-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-rose-500"
              />

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectId(null)}
                  className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded-lg text-xs hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingAction}
                  className="px-3 py-1.5 bg-rose-600 text-white rounded-lg text-xs font-semibold hover:bg-rose-700 disabled:opacity-50"
                >
                  Confirm Rejection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
