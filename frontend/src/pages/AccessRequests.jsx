import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { AccessRequestStatusBadge } from '../components/common/StatusBadge';
import {
  canRequestSharedAccess,
  canManageSharing,
  isInstitutionAdmin,
  isSystemAdmin
} from '../utils/roleHelper';
import {
  Share2,
  Calendar,
  Clock,
  User,
  Building,
  Check,
  X,
  AlertCircle,
  CheckCircle2,
  FileText,
  ArrowRight,
  Shield,
  Layers,
  Inbox,
  Send,
  HelpCircle
} from 'lucide-react';
import { format } from 'date-fns';

export default function AccessRequests() {
  const { user } = useAuth();

  const isIAOrAdmin = canManageSharing(user);
  const [activeTab, setActiveTab] = useState(isIAOrAdmin ? 'incoming' : 'my');

  const [myRequests, setMyRequests] = useState([]);
  const [outgoingRequests, setOutgoingRequests] = useState([]);
  const [incomingRequests, setIncomingRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');

  // Action Dialog Modals
  const [forwardDialog, setForwardDialog] = useState({ open: false, requestId: null, notes: '' });
  const [rejectDialog, setRejectDialog] = useState({ open: false, requestId: null, isHost: false, reason: '' });
  const [approveDialog, setApproveDialog] = useState({
    open: false,
    requestId: null,
    validFrom: new Date().toISOString().slice(0, 10),
    validTo: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
    notes: '',
  });
  const [submittingAction, setSubmittingAction] = useState(false);

  const fetchAllData = () => {
    setLoading(true);
    setError('');

    const promises = [];

    // Always fetch My Requests
    promises.push(
      api.get('/sharing/requests/my')
        .then((res) => {
          if (res.success && res.data) setMyRequests(res.data);
        })
        .catch((e) => console.warn('Failed to load my requests', e))
    );

    // Fetch Outgoing and Incoming if Institution Admin or System Admin
    if (isIAOrAdmin) {
      promises.push(
        api.get('/sharing/requests/outgoing')
          .then((res) => {
            if (res.success && res.data) setOutgoingRequests(res.data);
          })
          .catch((e) => console.warn('Failed to load outgoing requests', e)),
        api.get('/sharing/requests/incoming')
          .then((res) => {
            if (res.success && res.data) setIncomingRequests(res.data);
          })
          .catch((e) => console.warn('Failed to load incoming requests', e))
      );
    }

    Promise.all(promises).finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAllData();
  }, []);

  const handleForward = async (e) => {
    e.preventDefault();
    setSubmittingAction(true);
    try {
      const res = await api.put(`/sharing/requests/${forwardDialog.requestId}/forward`, {
        notes: forwardDialog.notes || 'Endorsed by home institution administrator.',
      });
      if (res.success) {
        setActionSuccess('Request endorsed and successfully forwarded to host institution!');
        setForwardDialog({ open: false, requestId: null, notes: '' });
        fetchAllData();
        setTimeout(() => setActionSuccess(''), 3500);
      }
    } catch (err) {
      alert(err.message || 'Failed to forward access request.');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleApproveHost = async (e) => {
    e.preventDefault();
    setSubmittingAction(true);
    try {
      const res = await api.put(`/sharing/requests/${approveDialog.requestId}/approve`, {
        validFrom: approveDialog.validFrom,
        validTo: approveDialog.validTo,
        notes: approveDialog.notes || 'Approved for inter-institution research usage.',
      });
      if (res.success) {
        setActionSuccess('Access request approved! Bilateral agreement and Access Grant generated.');
        setApproveDialog({ open: false, requestId: null, validFrom: '', validTo: '', notes: '' });
        fetchAllData();
        setTimeout(() => setActionSuccess(''), 3500);
      }
    } catch (err) {
      alert(err.message || 'Failed to approve access request.');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleReject = async (e) => {
    e.preventDefault();
    setSubmittingAction(true);
    const endpoint = rejectDialog.isHost
      ? `/sharing/requests/${rejectDialog.requestId}/reject-host`
      : `/sharing/requests/${rejectDialog.requestId}/reject-home`;

    try {
      const res = await api.put(endpoint, {
        notes: rejectDialog.reason || 'Declined according to institutional sharing policy.',
      });
      if (res.success) {
        setActionSuccess('Access request rejected.');
        setRejectDialog({ open: false, requestId: null, isHost: false, reason: '' });
        fetchAllData();
        setTimeout(() => setActionSuccess(''), 3500);
      }
    } catch (err) {
      alert(err.message || 'Failed to reject access request.');
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleWithdraw = async (id) => {
    if (!window.confirm('Are you sure you want to withdraw this access request?')) return;
    setSubmittingAction(true);
    try {
      const res = await api.put(`/sharing/requests/${id}/withdraw`);
      if (res.success) {
        setActionSuccess('Access request withdrawn.');
        fetchAllData();
        setTimeout(() => setActionSuccess(''), 3500);
      }
    } catch (err) {
      alert(err.message || 'Failed to withdraw access request.');
    } finally {
      setSubmittingAction(false);
    }
  };

  const currentList =
    activeTab === 'my'
      ? myRequests
      : activeTab === 'outgoing'
      ? outgoingRequests
      : incomingRequests;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Share2 className="w-7 h-7 text-teal-700" />
            Inter-Institution Access Requests
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Manage the two-step verification workflow for sharing laboratory equipment across institutions.
          </p>
        </div>

        {/* Tab Controls */}
        <div className="flex p-1 bg-slate-100 rounded-xl border border-slate-200 text-xs font-semibold">
          <button
            onClick={() => setActiveTab('my')}
            className={`px-3 py-1.5 rounded-lg transition ${
              activeTab === 'my' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            My Applications ({myRequests.length})
          </button>

          {isIAOrAdmin && (
            <>
              <button
                onClick={() => setActiveTab('outgoing')}
                className={`px-3 py-1.5 rounded-lg transition flex items-center gap-1 ${
                  activeTab === 'outgoing' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Send className="w-3.5 h-3.5" />
                <span>Outgoing Endorsements ({outgoingRequests.filter(r => r.status === 'PENDING_HOME_APPROVAL').length})</span>
              </button>

              <button
                onClick={() => setActiveTab('incoming')}
                className={`px-3 py-1.5 rounded-lg transition flex items-center gap-1 ${
                  activeTab === 'incoming' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Inbox className="w-3.5 h-3.5" />
                <span>Incoming Host Reviews ({incomingRequests.filter(r => r.status === 'FORWARDED_TO_HOST').length})</span>
              </button>
            </>
          )}
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
      ) : currentList.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <Share2 className="w-12 h-12 text-slate-300 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">
            {activeTab === 'my'
              ? 'No Access Applications'
              : activeTab === 'outgoing'
              ? 'No Outgoing Requests Awaiting Review'
              : 'No Incoming Partner Requests Awaiting Review'}
          </h3>
          <p className="text-sm text-slate-500 mt-1 max-w-sm mx-auto">
            {activeTab === 'my'
              ? 'You have not submitted any inter-institution equipment access requests.'
              : 'There are no active requests in this queue.'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden divide-y divide-slate-200">
          {currentList.map((item) => {
            const eq = item.listing?.equipment;
            const hostInst = eq?.department?.institution?.name || 'Host Institution';
            const requesterInst = item.requesterInstitution?.name || 'Home Institution';

            return (
              <div key={item.id} className="p-6 hover:bg-slate-50/50 transition flex flex-col lg:flex-row lg:items-start justify-between gap-6">
                <div className="space-y-3 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-base font-bold text-slate-900">{eq?.name}</span>
                    <AccessRequestStatusBadge status={item.status} />
                    <span className="text-xs text-slate-500 font-mono">Request #{item.id}</span>
                  </div>

                  {/* Institutions Flow */}
                  <div className="flex flex-wrap items-center gap-2 text-xs bg-slate-50 p-2.5 rounded-xl border border-slate-200/80 max-w-2xl">
                    <span className="font-semibold text-slate-800">{requesterInst}</span>
                    <ArrowRight className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-semibold text-teal-800">{hostInst}</span>
                    <span className="text-slate-300">|</span>
                    <span className="text-slate-600">Rate: ${item.listing?.feePerHour}/hr</span>
                    <span className="text-slate-300">|</span>
                    <span className="text-slate-600">Est. Hours: {item.estimatedUsageHours} hrs</span>
                  </div>

                  {/* Requester details */}
                  <div className="flex items-center space-x-2 text-xs text-slate-700">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-semibold">{item.requester.fullName}</span>
                    <span className="text-slate-400">•</span>
                    <span>{item.requester.email}</span>
                  </div>

                  {/* Research Purpose */}
                  <div className="text-xs text-slate-700 max-w-3xl leading-relaxed">
                    <span className="font-semibold text-slate-800">Research Purpose:</span> {item.purpose}
                  </div>

                  {/* Decisions notes if present */}
                  {item.homeDecisionNotes && (
                    <div className="text-[11px] p-2 bg-slate-100 rounded-lg text-slate-700 max-w-2xl">
                      <span className="font-bold text-slate-800">Home Admin Note:</span> {item.homeDecisionNotes}
                    </div>
                  )}

                  {item.hostDecisionNotes && (
                    <div className="text-[11px] p-2 bg-teal-50 border border-teal-100 rounded-lg text-teal-900 max-w-2xl">
                      <span className="font-bold text-teal-800">Host Admin Note:</span> {item.hostDecisionNotes}
                      {item.grantedValidTo && (
                        <span className="block mt-0.5 font-medium">
                          Valid From: {item.grantedValidFrom} to {item.grantedValidTo}
                        </span>
                      )}
                    </div>
                  )}
                </div>

                {/* Actions per tab */}
                <div className="flex items-center space-x-2 flex-shrink-0">
                  {/* Tab 1: My Requests */}
                  {activeTab === 'my' && (
                    <>
                      {(item.status === 'PENDING_HOME_APPROVAL' || item.status === 'FORWARDED_TO_HOST') && (
                        <button
                          onClick={() => handleWithdraw(item.id)}
                          disabled={submittingAction}
                          className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-semibold transition"
                        >
                          Withdraw
                        </button>
                      )}
                      {item.status === 'APPROVED' && (
                        <span className="text-xs font-semibold text-emerald-700 flex items-center gap-1 bg-emerald-50 px-3 py-1.5 rounded-xl border border-emerald-200">
                          <Check className="w-3.5 h-3.5" />
                          Access Granted
                        </span>
                      )}
                    </>
                  )}

                  {/* Tab 2: Outgoing Requests (Home Admin Action) */}
                  {activeTab === 'outgoing' && item.status === 'PENDING_HOME_APPROVAL' && (
                    <>
                      <button
                        onClick={() => setForwardDialog({ open: true, requestId: item.id, notes: '' })}
                        disabled={submittingAction}
                        className="px-3.5 py-2 bg-teal-700 hover:bg-teal-800 text-white rounded-xl text-xs font-semibold shadow-sm transition flex items-center gap-1"
                      >
                        <Send className="w-3.5 h-3.5" />
                        <span>Endorse &amp; Forward</span>
                      </button>

                      <button
                        onClick={() => setRejectDialog({ open: true, requestId: item.id, isHost: false, reason: '' })}
                        disabled={submittingAction}
                        className="px-3 py-2 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-semibold transition"
                      >
                        Decline
                      </button>
                    </>
                  )}

                  {/* Tab 3: Incoming Requests (Host Admin Action) */}
                  {activeTab === 'incoming' && item.status === 'FORWARDED_TO_HOST' && (
                    <>
                      <button
                        onClick={() =>
                          setApproveDialog({
                            open: true,
                            requestId: item.id,
                            validFrom: new Date().toISOString().slice(0, 10),
                            validTo: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
                            notes: '',
                          })
                        }
                        disabled={submittingAction}
                        className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold shadow-sm transition flex items-center gap-1"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Approve Access</span>
                      </button>

                      <button
                        onClick={() => setRejectDialog({ open: true, requestId: item.id, isHost: true, reason: '' })}
                        disabled={submittingAction}
                        className="px-3 py-2 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-semibold transition"
                      >
                        Decline
                      </button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Forward Endorsement Dialog */}
      {forwardDialog.open && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 mb-2">Endorse &amp; Forward Request to Host</h3>
            <p className="text-xs text-slate-600 mb-4">
              As Home Institution Administrator, verify that the researcher has legitimate academic justification and billing clearance to access external resources.
            </p>

            <form onSubmit={handleForward} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Endorsement Notes / Account Clearance (Optional)
                </label>
                <textarea
                  rows={3}
                  value={forwardDialog.notes}
                  onChange={(e) => setForwardDialog({ ...forwardDialog, notes: e.target.value })}
                  placeholder="e.g. Approved under Department Grant #XYZ; funding verified for usage fees."
                  className="w-full p-2.5 border border-slate-300 rounded-xl text-xs"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setForwardDialog({ open: false, requestId: null, notes: '' })}
                  className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingAction}
                  className="px-4 py-1.5 bg-teal-700 text-white rounded-xl text-xs font-semibold hover:bg-teal-800 disabled:opacity-50"
                >
                  Confirm Forward
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Host Approval Dialog */}
      {approveDialog.open && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 mb-2">Approve Equipment Access Grant</h3>
            <p className="text-xs text-slate-600 mb-4">
              Specify the validity window for this access grant. An active bilateral agreement and access credential will be issued for the researcher.
            </p>

            <form onSubmit={handleApproveHost} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">Valid From</label>
                  <input
                    type="date"
                    required
                    value={approveDialog.validFrom}
                    onChange={(e) => setApproveDialog({ ...approveDialog, validFrom: e.target.value })}
                    className="w-full p-2 border border-slate-300 rounded-xl text-xs"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-700 mb-1">Valid To</label>
                  <input
                    type="date"
                    required
                    value={approveDialog.validTo}
                    onChange={(e) => setApproveDialog({ ...approveDialog, validTo: e.target.value })}
                    className="w-full p-2 border border-slate-300 rounded-xl text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Host Agreement Terms &amp; Conditions (Optional)
                </label>
                <textarea
                  rows={2}
                  value={approveDialog.notes}
                  onChange={(e) => setApproveDialog({ ...approveDialog, notes: e.target.value })}
                  placeholder="e.g. Standard safety induction required upon first physical visit."
                  className="w-full p-2.5 border border-slate-300 rounded-xl text-xs"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setApproveDialog({ open: false, requestId: null, validFrom: '', validTo: '', notes: '' })}
                  className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingAction}
                  className="px-4 py-1.5 bg-emerald-600 text-white rounded-xl text-xs font-semibold hover:bg-emerald-700 disabled:opacity-50"
                >
                  Issue Access Grant
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reject Dialog */}
      {rejectDialog.open && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 mb-2">Decline Equipment Access Request</h3>
            <p className="text-xs text-slate-600 mb-4">
              Please provide a reason for declining this request. The applicant and partner institution will be informed.
            </p>

            <form onSubmit={handleReject} className="space-y-4">
              <textarea
                rows={3}
                required
                value={rejectDialog.reason}
                onChange={(e) => setRejectDialog({ ...rejectDialog, reason: e.target.value })}
                placeholder="e.g. Internal priority experiments booked; capacity quota currently exhausted."
                className="w-full p-2.5 border border-slate-300 rounded-xl text-xs focus:ring-rose-500"
              />

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectDialog({ open: false, requestId: null, isHost: false, reason: '' })}
                  className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingAction}
                  className="px-4 py-1.5 bg-rose-600 text-white rounded-xl text-xs font-semibold hover:bg-rose-700 disabled:opacity-50"
                >
                  Confirm Decline
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
