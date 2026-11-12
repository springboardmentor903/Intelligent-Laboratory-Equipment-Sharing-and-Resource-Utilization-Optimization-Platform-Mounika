import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { WaitlistStatusBadge } from '../components/common/StatusBadge';
import {
  ListOrdered,
  Clock,
  Calendar,
  MapPin,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ChevronRight,
  Sparkles,
  ArrowRight,
  RefreshCw
} from 'lucide-react';
import { format } from 'date-fns';

export default function MyWaitlist() {
  const [waitlistEntries, setWaitlistEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  // Status Filter: ALL, WAITING, OFFERED, ACCEPTED, DECLINED, EXPIRED, CANCELLED
  const [statusFilter, setStatusFilter] = useState('');

  // Accept Offer Modal
  const [acceptModalOpen, setAcceptModalOpen] = useState(false);
  const [selectedOffer, setSelectedOffer] = useState(null);
  const [purposeInput, setPurposeInput] = useState('');
  const [accepting, setAccepting] = useState(false);

  const fetchWaitlist = () => {
    setLoading(true);
    const params = statusFilter ? { status: statusFilter } : {};
    api.get('/waitlist/my', { params })
      .then((res) => {
        if (res.success && res.data) {
          setWaitlistEntries(res.data.content || []);
          setError('');
        }
      })
      .catch((err) => setError(err.message || 'Failed to load waitlist entries.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchWaitlist();

    // 1-second interval to decrement remaining countdown seconds locally
    const timer = setInterval(() => {
      setWaitlistEntries((prev) =>
        prev.map((item) => {
          if (item.status === 'OFFERED' && item.remainingSeconds > 0) {
            const nextSec = item.remainingSeconds - 1;
            if (nextSec === 0) {
              fetchWaitlist(); // Reload when countdown hits zero
            }
            return { ...item, remainingSeconds: nextSec };
          }
          return item;
        })
      );
    }, 1000);

    // Refresh every 60s
    const poll = setInterval(fetchWaitlist, 60000);

    return () => {
      clearInterval(timer);
      clearInterval(poll);
    };
  }, [statusFilter]);

  const handleOpenAcceptModal = (offer) => {
    setSelectedOffer(offer);
    setPurposeInput('');
    setAcceptModalOpen(true);
  };

  const handleConfirmAccept = async (e) => {
    e.preventDefault();
    if (!purposeInput.trim()) {
      alert('Please state your research/lab purpose.');
      return;
    }

    setAccepting(true);
    try {
      const res = await api.post(`/waitlist/${selectedOffer.id}/accept`, {
        purpose: purposeInput.trim(),
      });
      if (res.success) {
        setActionMessage('Offer accepted! Your reservation has been created.');
        setAcceptModalOpen(false);
        fetchWaitlist();
        setTimeout(() => setActionMessage(''), 4000);
      }
    } catch (err) {
      alert(err.message || 'Failed to accept offer.');
    } finally {
      setAccepting(false);
    }
  };

  const handleDeclineOffer = async (id) => {
    if (!window.confirm('Are you sure you want to decline this slot offer? It will be immediately offered to the next queued researcher.')) {
      return;
    }

    try {
      const res = await api.post(`/waitlist/${id}/decline`);
      if (res.success) {
        setActionMessage('Offer declined and slot released.');
        fetchWaitlist();
        setTimeout(() => setActionMessage(''), 3000);
      }
    } catch (err) {
      alert(err.message || 'Failed to decline offer.');
    }
  };

  const handleCancelWaitlist = async (id) => {
    if (!window.confirm('Are you sure you want to withdraw from this waitlist?')) {
      return;
    }

    try {
      const res = await api.post(`/waitlist/${id}/cancel`);
      if (res.success) {
        setActionMessage('You have left the waitlist.');
        fetchWaitlist();
        setTimeout(() => setActionMessage(''), 3000);
      }
    } catch (err) {
      alert(err.message || 'Failed to cancel waitlist entry.');
    }
  };

  const formatCountdown = (totalSec) => {
    if (!totalSec || totalSec <= 0) return '00:00';
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const activeOffers = waitlistEntries.filter((w) => w.status === 'OFFERED');
  const otherEntries = waitlistEntries.filter((w) => w.status !== 'OFFERED');

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2.5">
            <ListOrdered className="w-8 h-8 text-teal-700" />
            My Equipment Waitlist
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            FIFO priority queues for high-demand laboratory instruments and active slot release offers.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            to="/"
            className="inline-flex items-center px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white text-xs font-semibold rounded-xl shadow-sm transition"
          >
            Catalog
          </Link>
          <button
            onClick={fetchWaitlist}
            className="p-2 border border-slate-300 rounded-xl bg-white hover:bg-slate-50 text-slate-600 transition"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-teal-600' : ''}`} />
          </button>
        </div>
      </div>

      {actionMessage && (
        <div className="p-3 bg-teal-50 border border-teal-200 text-teal-800 text-xs rounded-xl flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          <span>{actionMessage}</span>
        </div>
      )}

      {/* ACTIVE OFFERS HIGHLIGHT BANNER */}
      {activeOffers.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-sm font-bold uppercase tracking-wider text-emerald-800 flex items-center gap-1.5">
            <Sparkles className="w-4 h-4 text-emerald-600" />
            Active Slot Offers Available Now! ({activeOffers.length})
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {activeOffers.map((offer) => (
              <div
                key={offer.id}
                className="p-5 bg-gradient-to-br from-emerald-50 to-teal-50 border-2 border-emerald-400 rounded-2xl shadow-sm space-y-3"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-[10px] font-mono font-bold px-2 py-0.5 bg-emerald-200 text-emerald-900 rounded">
                      SLOT OFFERED
                    </span>
                    <h3 className="text-base font-bold text-slate-900 mt-1">
                      {offer.equipmentName}
                    </h3>
                    <p className="text-xs text-slate-600">Tag: {offer.assetTag}</p>
                  </div>

                  <div className="text-right">
                    <span className="text-[11px] font-semibold text-rose-700 block">Offer Expires In:</span>
                    <span className="text-lg font-mono font-extrabold text-rose-800 bg-rose-50 px-2.5 py-0.5 rounded-lg border border-rose-200">
                      {formatCountdown(offer.remainingSeconds)}
                    </span>
                  </div>
                </div>

                <div className="text-xs text-slate-700 space-y-1">
                  <div className="flex items-center space-x-1.5">
                    <Calendar className="w-3.5 h-3.5 text-teal-700" />
                    <span className="font-semibold">
                      {format(new Date(offer.requestedStartTime), 'EEE, MMM d, yyyy')}
                    </span>
                  </div>
                  <div className="flex items-center space-x-1.5">
                    <Clock className="w-3.5 h-3.5 text-teal-700" />
                    <span>
                      {format(new Date(offer.requestedStartTime), 'h:mm a')} –{' '}
                      {format(new Date(offer.requestedEndTime), 'h:mm a')}
                    </span>
                  </div>
                </div>

                <div className="pt-2 border-t border-emerald-200 flex items-center justify-end space-x-2">
                  <button
                    onClick={() => handleDeclineOffer(offer.id)}
                    className="px-3 py-1.5 border border-slate-300 hover:bg-white text-slate-700 rounded-xl text-xs font-semibold transition"
                  >
                    Decline
                  </button>
                  <button
                    onClick={() => handleOpenAcceptModal(offer)}
                    className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow transition"
                  >
                    Accept Slot &amp; Book
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setStatusFilter('')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            statusFilter === '' ? 'bg-teal-700 text-white' : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          All Entries
        </button>
        <button
          onClick={() => setStatusFilter('WAITING')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            statusFilter === 'WAITING' ? 'bg-teal-700 text-white' : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          In Queue (Waiting)
        </button>
        <button
          onClick={() => setStatusFilter('ACCEPTED')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            statusFilter === 'ACCEPTED' ? 'bg-teal-700 text-white' : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Accepted
        </button>
        <button
          onClick={() => setStatusFilter('EXPIRED')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            statusFilter === 'EXPIRED' ? 'bg-teal-700 text-white' : 'text-slate-600 hover:bg-slate-100'
          }`}
        >
          Expired
        </button>
      </div>

      {/* Main Waitlist Table */}
      {loading ? (
        <div className="flex justify-center items-center py-16">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600"></div>
        </div>
      ) : waitlistEntries.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <ListOrdered className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-base font-bold text-slate-800">Your Waitlist Is Empty</h3>
          <p className="text-xs text-slate-500 mt-1">
            When a desired equipment reservation slot is occupied, click &quot;Join Waitlist&quot; to queue for released time.
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="divide-y divide-slate-100">
            {otherEntries.map((item) => {
              const start = new Date(item.requestedStartTime);
              const end = new Date(item.requestedEndTime);
              const isWaiting = item.status === 'WAITING';

              return (
                <div key={item.id} className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-slate-50/70 transition">
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center space-x-3 flex-wrap gap-y-1">
                      <Link
                        to={`/equipment/${item.equipmentId}`}
                        className="text-sm font-bold text-slate-900 hover:text-teal-700"
                      >
                        {item.equipmentName}
                      </Link>
                      <span className="text-xs font-mono bg-slate-100 px-2 py-0.5 rounded text-slate-600">
                        {item.assetTag}
                      </span>
                      <WaitlistStatusBadge status={item.status} />

                      {isWaiting && (
                        <span className="px-2.5 py-0.5 bg-amber-50 text-amber-800 border border-amber-200 rounded-full text-xs font-bold">
                          Queue Position: #{item.queuePosition}
                        </span>
                      )}
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 text-xs text-slate-600">
                      <div className="flex items-center">
                        <Calendar className="w-3.5 h-3.5 mr-1 text-teal-600" />
                        <span>{format(start, 'EEE, MMM d, yyyy')}</span>
                      </div>
                      <div className="flex items-center">
                        <Clock className="w-3.5 h-3.5 mr-1 text-teal-600" />
                        <span>{format(start, 'h:mm a')} – {format(end, 'h:mm a')}</span>
                      </div>
                      <div className="flex items-center">
                        <MapPin className="w-3.5 h-3.5 mr-1 text-slate-400" />
                        <span>{item.locationRoom || 'Main Lab'}</span>
                      </div>
                    </div>
                  </div>

                  {isWaiting && (
                    <div className="flex items-center space-x-2 flex-shrink-0">
                      <button
                        onClick={() => handleCancelWaitlist(item.id)}
                        className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-xl text-xs font-semibold transition"
                      >
                        Leave Waitlist
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Accept Offer Purpose Modal */}
      {acceptModalOpen && selectedOffer && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div>
              <h3 className="text-lg font-bold text-slate-900">Accept Waitlist Reservation Offer</h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Instrument: <strong className="text-slate-800">{selectedOffer.equipmentName}</strong> ({selectedOffer.assetTag})
              </p>
            </div>

            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
              <div>
                <strong>Reserved Window:</strong>{' '}
                {format(new Date(selectedOffer.requestedStartTime), 'EEE, MMM d • h:mm a')} –{' '}
                {format(new Date(selectedOffer.requestedEndTime), 'h:mm a')}
              </div>
            </div>

            <form onSubmit={handleConfirmAccept} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Research / Booking Purpose <span className="text-rose-500">*</span>
                </label>
                <textarea
                  rows={3}
                  required
                  maxLength={500}
                  value={purposeInput}
                  onChange={(e) => setPurposeInput(e.target.value)}
                  placeholder="Detail your research experiment, project title, or lab procedure..."
                  className="w-full p-3 border border-slate-300 rounded-xl text-xs focus:ring-2 focus:ring-teal-600 focus:outline-none"
                />
                <span className="text-[10px] text-slate-400 block text-right">{purposeInput.length}/500</span>
              </div>

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  disabled={accepting}
                  onClick={() => setAcceptModalOpen(false)}
                  className="px-4 py-2 border border-slate-300 rounded-xl text-xs font-semibold text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={accepting || !purposeInput.trim()}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow"
                >
                  {accepting ? 'Confirming...' : 'Confirm & Create Booking'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
