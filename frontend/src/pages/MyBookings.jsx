import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { BookingStatusBadge } from '../components/common/StatusBadge';
import { Calendar, Clock, MapPin, AlertCircle, XCircle, CheckCircle2, ChevronRight, Beaker, Play, Square, Repeat, Zap } from 'lucide-react';
import { format } from 'date-fns';

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [processingId, setProcessingId] = useState(null);

  const fetchBookings = () => {
    setLoading(true);
    api.get('/bookings/my')
      .then((res) => {
        if (res.success && res.data) {
          setBookings(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load reservations.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchBookings();
    const interval = setInterval(fetchBookings, 30000); // Poll every 30s as fallback
    return () => clearInterval(interval);
  }, []);

  const handleCheckIn = async (bookingId) => {
    setProcessingId(bookingId);
    try {
      const res = await api.post(`/bookings/${bookingId}/check-in`);
      if (res.success) {
        setActionMessage('Session started! You have checked in to your equipment.');
        fetchBookings();
        setTimeout(() => setActionMessage(''), 4000);
      }
    } catch (err) {
      alert(err.message || 'Check-in failed.');
    } finally {
      setProcessingId(null);
    }
  };

  const handleCheckOut = async (bookingId) => {
    if (!window.confirm('Are you ready to check out and conclude your lab session?')) {
      return;
    }
    setProcessingId(bookingId);
    try {
      const res = await api.post(`/bookings/${bookingId}/check-out`);
      if (res.success) {
        setActionMessage('Session completed! Equipment is now freed and available.');
        fetchBookings();
        setTimeout(() => setActionMessage(''), 4000);
      }
    } catch (err) {
      alert(err.message || 'Check-out failed.');
    } finally {
      setProcessingId(null);
    }
  };

  const handleShiftToNow = async (bookingId) => {
    setProcessingId(bookingId);
    try {
      const res = await api.post(`/bookings/${bookingId}/shift-to-now`);
      if (res.success) {
        setActionMessage('Booking shifted to right now! You can now check in.');
        fetchBookings();
        setTimeout(() => setActionMessage(''), 4000);
      }
    } catch (err) {
      alert(err.message || 'Failed to shift booking time.');
    } finally {
      setProcessingId(null);
    }
  };

  const handleCancel = async (booking) => {
    if (booking.recurrenceId) {
      const choice = window.confirm(
        'This booking belongs to a recurring series.\n\nClick OK to cancel ALL future bookings in this series, or CANCEL to only cancel this single session.'
      );
      try {
        if (choice) {
          const res = await api.post(`/bookings/series/${booking.recurrenceId}/cancel`, {
            reason: 'Cancelled series by user'
          });
          if (res.success) {
            setActionMessage('Recurring series cancelled successfully.');
            fetchBookings();
            setTimeout(() => setActionMessage(''), 3000);
          }
          return;
        }
      } catch (err) {
        alert(err.message || 'Failed to cancel recurring series.');
        return;
      }
    }

    if (!window.confirm('Are you sure you want to cancel this equipment booking?')) {
      return;
    }

    try {
      const res = await api.put(`/bookings/${booking.id}/cancel`, {
        reason: 'Cancelled by researcher',
      });
      if (res.success) {
        setActionMessage('Reservation cancelled successfully.');
        fetchBookings();
        setTimeout(() => setActionMessage(''), 3000);
      }
    } catch (err) {
      alert(err.message || 'Failed to cancel booking.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            My Reservations & Bookings
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Track your scheduled equipment sessions, live check-in windows, and usage history.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <Link
            to="/monitoring"
            className="inline-flex items-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-xl shadow-sm transition"
          >
            <Clock className="w-4 h-4 mr-1.5" />
            Live Equipment Monitor
          </Link>
          <Link
            to="/"
            className="inline-flex items-center px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white text-xs font-semibold rounded-xl shadow-sm transition"
          >
            <Beaker className="w-4 h-4 mr-1.5" />
            Equipment Catalog
          </Link>
        </div>
      </div>

      {actionMessage && (
        <div className="p-3 bg-teal-50 border border-teal-200 text-teal-800 text-xs rounded-xl flex items-center space-x-2">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          <span>{actionMessage}</span>
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
      ) : bookings.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <Calendar className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">No Reservations Found</h3>
          <p className="text-sm text-slate-500 mt-1">You have not scheduled any laboratory equipment yet.</p>
          <Link
            to="/"
            className="mt-4 inline-flex items-center text-xs font-semibold text-teal-700 hover:text-teal-800"
          >
            Find equipment to book <ChevronRight className="w-4 h-4 ml-0.5" />
          </Link>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="divide-y divide-slate-200">
            {bookings.map((b) => {
              const start = new Date(b.startTime);
              const end = new Date(b.endTime);
              const now = new Date();

              const checkInOpen = new Date(start.getTime() - 15 * 60 * 1000);
              const isEligibleCheckIn = b.status === 'CONFIRMED' && now >= checkInOpen && now <= end;
              const isEligibleCheckOut = b.status === 'IN_USE';
              const canCancel = ['PENDING_APPROVAL', 'CONFIRMED'].includes(b.status) && now < start;

              return (
                <div key={b.id} className="p-6 hover:bg-slate-50 transition flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center space-x-3 flex-wrap gap-y-2">
                      <Link
                        to={`/equipment/${b.equipment.id}`}
                        className="text-base font-bold text-slate-900 hover:text-teal-700 transition"
                      >
                        {b.equipment.name}
                      </Link>
                      <span className="text-xs font-mono bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                        {b.equipment.assetTag}
                      </span>
                      <BookingStatusBadge status={b.status} />

                      {b.recurrenceId && (
                        <span className="inline-flex items-center text-xs font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 px-2 py-0.5 rounded-full">
                          <Repeat className="w-3 h-3 mr-1" /> Recurring Series
                        </span>
                      )}

                      {b.sharingAgreementId && (
                        <span className="inline-flex items-center text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
                          Inter-Institution Shared
                        </span>
                      )}
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-600">
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
                        <span>{b.equipment.locationRoom || 'Main Lab'}</span>
                      </div>
                    </div>

                    <p className="text-xs text-slate-600 max-w-2xl">
                      <span className="font-semibold text-slate-700">Purpose:</span> {b.purpose}
                    </p>

                    {b.estimatedFee && (
                      <p className="text-xs text-amber-800 font-medium">
                        Usage Fee: ${Number(b.estimatedFee).toFixed(2)}
                      </p>
                    )}

                    {b.rejectionReason && (
                      <p className="text-xs text-rose-600 font-medium bg-rose-50 p-2 rounded-lg inline-block">
                        Rejection Reason: {b.rejectionReason}
                      </p>
                    )}
                  </div>

                  <div className="flex flex-wrap items-center gap-2 flex-shrink-0">
                    {/* Check In Action Button */}
                    {isEligibleCheckIn && (
                      <button
                        onClick={() => handleCheckIn(b.id)}
                        disabled={processingId === b.id}
                        className="inline-flex items-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-lg text-xs font-bold shadow-md hover:shadow-lg transition animate-pulse"
                      >
                        <Play className="w-3.5 h-3.5 mr-1.5 fill-white" />
                        {processingId === b.id ? 'Starting Session...' : 'Check In'}
                      </button>
                    )}

                    {/* If CONFIRMED but before the 15-minute check-in window */}
                    {b.status === 'CONFIRMED' && !isEligibleCheckIn && now < checkInOpen && (
                      <div className="flex flex-col items-end gap-1">
                        <span className="inline-flex items-center text-xs font-semibold text-amber-800 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-lg">
                          <Clock className="w-3.5 h-3.5 mr-1 text-amber-600" />
                          Check-in opens {format(checkInOpen, 'h:mm a')}
                        </span>
                        <div className="flex items-center gap-1.5">
                          <span className="text-[10px] text-slate-400">15 min before session</span>
                          <button
                            type="button"
                            onClick={() => handleShiftToNow(b.id)}
                            disabled={processingId === b.id}
                            className="inline-flex items-center text-[10px] font-bold text-teal-700 hover:text-teal-900 bg-teal-50 hover:bg-teal-100 border border-teal-300 px-2 py-0.5 rounded shadow-2xs transition"
                            title="Shift this reservation to right now so you can click Check-In immediately"
                          >
                            <Zap className="w-3 h-3 mr-0.5 text-teal-600" /> Demo: Shift to Now
                          </button>
                        </div>
                      </div>
                    )}

                    {/* If CONFIRMED but session already passed */}
                    {b.status === 'CONFIRMED' && !isEligibleCheckIn && now > end && (
                      <span className="text-xs font-medium text-slate-500 bg-slate-100 border border-slate-200 px-2.5 py-1 rounded-lg">
                        Session time elapsed
                      </span>
                    )}

                    {/* Check Out Action Button */}
                    {isEligibleCheckOut && (
                      <button
                        onClick={() => handleCheckOut(b.id)}
                        disabled={processingId === b.id}
                        className="inline-flex items-center px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 disabled:opacity-50 text-white rounded-lg text-xs font-semibold shadow-sm transition"
                      >
                        <Square className="w-3.5 h-3.5 mr-1 fill-white" />
                        {processingId === b.id ? 'Concluding...' : 'Check Out'}
                      </button>
                    )}

                    {/* Cancel Booking Action */}
                    {canCancel && (
                      <button
                        onClick={() => handleCancel(b)}
                        className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-lg text-xs font-semibold transition"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
