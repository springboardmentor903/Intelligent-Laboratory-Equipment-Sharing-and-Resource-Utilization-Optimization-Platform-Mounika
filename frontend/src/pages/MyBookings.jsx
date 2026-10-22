import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { BookingStatusBadge } from '../components/common/StatusBadge';
import { Calendar, Clock, MapPin, AlertCircle, XCircle, CheckCircle2, ChevronRight, Beaker } from 'lucide-react';
import { format } from 'date-fns';

export default function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

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
  }, []);

  const handleCancel = async (bookingId) => {
    if (!window.confirm('Are you sure you want to cancel this equipment booking?')) {
      return;
    }

    try {
      const res = await api.put(`/bookings/${bookingId}/cancel`, {
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
            Track your scheduled equipment sessions, approval statuses, and reservation history.
          </p>
        </div>

        <Link
          to="/"
          className="inline-flex items-center px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white text-xs font-semibold rounded-xl shadow-sm transition"
        >
          <Beaker className="w-4 h-4 mr-1.5" />
          Browse Equipment Catalog
        </Link>
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
              const canCancel = ['PENDING_APPROVAL', 'CONFIRMED'].includes(b.status);

              return (
                <div key={b.id} className="p-6 hover:bg-slate-50 transition flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center space-x-3">
                      <Link
                        to={`/equipment/${b.equipment.id}`}
                        className="text-base font-bold text-slate-900 hover:text-teal-700 transition"
                      >
                        {b.equipment.name}
                      </Link>
                      <BookingStatusBadge status={b.status} />
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
                        <span>{b.equipment.locationRoom}</span>
                      </div>
                    </div>

                    <p className="text-xs text-slate-600 max-w-2xl">
                      <span className="font-semibold text-slate-700">Purpose:</span> {b.purpose}
                    </p>

                    {b.notes && (
                      <p className="text-xs text-slate-500 italic">
                        Notes: {b.notes}
                      </p>
                    )}

                    {b.rejectionReason && (
                      <p className="text-xs text-rose-600 font-medium bg-rose-50 p-2 rounded-lg inline-block">
                        Rejection Reason: {b.rejectionReason}
                      </p>
                    )}
                  </div>

                  {canCancel && (
                    <div className="flex-shrink-0">
                      <button
                        onClick={() => handleCancel(b.id)}
                        className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-lg text-xs font-semibold transition"
                      >
                        Cancel Booking
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
