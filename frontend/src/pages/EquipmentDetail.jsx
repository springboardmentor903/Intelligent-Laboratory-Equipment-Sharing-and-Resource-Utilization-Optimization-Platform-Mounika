import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { EquipmentStatusBadge } from '../components/common/StatusBadge';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import {
  Beaker,
  Calendar,
  Clock,
  MapPin,
  Building,
  Shield,
  DollarSign,
  Share2,
  FileText,
  AlertCircle,
  CheckCircle2,
  X,
  ArrowLeft
} from 'lucide-react';

export default function EquipmentDetail() {
  const { id } = useParams();
  const { user, isAuthenticated } = useAuth();

  const [equipment, setEquipment] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Booking Modal State
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState('');
  const [bookingError, setBookingError] = useState('');
  const [bookingForm, setBookingForm] = useState({
    startTime: '',
    endTime: '',
    purpose: '',
    notes: '',
  });

  const fetchEquipmentDetails = () => {
    setLoading(true);
    api.get(`/equipment/${id}`)
      .then((res) => {
        if (res.success && res.data) {
          setEquipment(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load equipment details.'))
      .finally(() => setLoading(false));
  };

  const fetchCalendarEvents = () => {
    api.get(`/bookings/equipment/${id}/calendar`)
      .then((res) => {
        if (res.success && res.data) {
          setEvents(res.data);
        }
      })
      .catch((err) => console.error('Failed to load calendar events:', err));
  };

  useEffect(() => {
    fetchEquipmentDetails();
    fetchCalendarEvents();
  }, [id]);

  const handleDateSelect = (selectInfo) => {
    if (!isAuthenticated) return;
    // Auto-fill selected start and end time from calendar selection
    const startIso = selectInfo.startStr.slice(0, 16);
    const endIso = selectInfo.endStr.slice(0, 16);

    setBookingForm({
      startTime: startIso,
      endTime: endIso,
      purpose: '',
      notes: '',
    });
    setBookingError('');
    setBookingSuccess('');
    setShowBookingModal(true);
  };

  const handleOpenBookingModal = () => {
    // Default to tomorrow 10:00 to 12:00
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const startStr = tomorrow.toISOString().slice(0, 11) + '10:00';
    const endStr = tomorrow.toISOString().slice(0, 11) + '12:00';

    setBookingForm({
      startTime: startStr,
      endTime: endStr,
      purpose: '',
      notes: '',
    });
    setBookingError('');
    setBookingSuccess('');
    setShowBookingModal(true);
  };

  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    setBookingError('');
    setBookingSuccess('');
    setBookingLoading(true);

    try {
      const res = await api.post('/bookings', {
        equipmentId: Number(id),
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        purpose: bookingForm.purpose,
        notes: bookingForm.notes,
      });

      if (res.success) {
        setBookingSuccess('Booking reservation request submitted! Awaiting lab manager approval.');
        fetchCalendarEvents();
        setTimeout(() => {
          setShowBookingModal(false);
          setBookingSuccess('');
        }, 2000);
      }
    } catch (err) {
      // Catches 409 Conflict (collision) or 400 Bad Request
      setBookingError(err.message || 'Failed to submit booking reservation.');
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-600"></div>
      </div>
    );
  }

  if (error || !equipment) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 text-center">
        <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-3" />
        <h2 className="text-xl font-bold text-slate-800">Equipment Not Found</h2>
        <p className="text-slate-600 mt-1">{error || 'Unable to retrieve requested equipment details.'}</p>
        <Link to="/" className="inline-flex items-center text-teal-700 font-medium mt-4">
          <ArrowLeft className="w-4 h-4 mr-1" /> Back to Catalog
        </Link>
      </div>
    );
  }

  const isBookable = equipment.status === 'AVAILABLE';

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-8">
      {/* Breadcrumb & Navigation */}
      <div>
        <Link to="/" className="inline-flex items-center text-sm font-medium text-slate-500 hover:text-teal-700 transition">
          <ArrowLeft className="w-4 h-4 mr-1.5" /> Back to Equipment Catalog
        </Link>
      </div>

      {/* Main Details Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="p-6 sm:p-8">
          <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
            <div className="space-y-3 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-teal-800 bg-teal-50 px-2.5 py-1 rounded-md">
                  {equipment.category}
                </span>
                <EquipmentStatusBadge status={equipment.status} />
                {equipment.sharableInterInstitution && (
                  <span className="text-xs font-medium text-emerald-800 bg-emerald-50 px-2.5 py-1 rounded-md flex items-center gap-1">
                    <Share2 className="w-3.5 h-3.5" /> Inter-Institution Sharing
                  </span>
                )}
              </div>

              <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                {equipment.name}
              </h1>

              <p className="text-xs sm:text-sm text-slate-500 font-mono">
                Model: {equipment.modelNumber || 'N/A'} • Serial No: {equipment.serialNumber || 'N/A'}
              </p>

              <p className="text-sm text-slate-700 leading-relaxed max-w-3xl">
                {equipment.description}
              </p>
            </div>

            {/* Action Box */}
            <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200 min-w-[260px] text-center lg:text-right space-y-4">
              <div>
                <span className="text-xs text-slate-500 block uppercase font-medium">Standard Usage Rate</span>
                <span className="text-3xl font-extrabold text-slate-900">${equipment.hourlyRate}</span>
                <span className="text-xs text-slate-500"> / hour</span>
              </div>

              {isAuthenticated ? (
                <button
                  onClick={handleOpenBookingModal}
                  disabled={!isBookable}
                  className={`w-full py-3 px-4 rounded-xl text-sm font-semibold shadow-sm transition flex items-center justify-center space-x-2 ${
                    isBookable
                      ? 'bg-teal-700 hover:bg-teal-800 text-white'
                      : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                  }`}
                >
                  <Calendar className="w-4 h-4" />
                  <span>{isBookable ? 'Book Time Slot' : `Not Bookable (${equipment.status})`}</span>
                </button>
              ) : (
                <Link
                  to="/login"
                  className="w-full inline-block py-3 px-4 rounded-xl text-sm font-semibold text-white bg-teal-700 hover:bg-teal-800 shadow-sm transition"
                >
                  Sign In to Reserve
                </Link>
              )}

              <p className="text-[11px] text-slate-400">
                Reservations are subject to lab manager review and calendar availability.
              </p>
            </div>
          </div>

          {/* Key Specifications & Metadata */}
          <div className="mt-8 pt-6 border-t border-slate-200 grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
                Facility & Location
              </span>
              <div className="space-y-1 text-sm text-slate-800">
                <div className="flex items-center">
                  <MapPin className="w-4 h-4 mr-2 text-teal-600" />
                  <span>{equipment.locationRoom || 'Main Lab Room'}</span>
                </div>
                <div className="flex items-center">
                  <Building className="w-4 h-4 mr-2 text-teal-600" />
                  <span>{equipment.department?.name}</span>
                </div>
                <div className="text-xs text-slate-500 pl-6">
                  {equipment.institution?.name}
                </div>
              </div>
            </div>

            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
                Calibration Status
              </span>
              <div className="space-y-1 text-xs text-slate-700">
                <p>
                  <span className="text-slate-500">Last Calibrated:</span>{' '}
                  <span className="font-semibold">{equipment.lastCalibratedAt || 'Verified on setup'}</span>
                </p>
                <p>
                  <span className="text-slate-500">Next Due:</span>{' '}
                  <span className="font-semibold">{equipment.nextCalibrationDue || 'Scheduled in 6 months'}</span>
                </p>
                <p className="text-[11px] text-emerald-700 font-medium">✓ Certified operational parameters</p>
              </div>
            </div>

            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
                Key Technical Specs
              </span>
              <p className="text-xs text-slate-700 font-mono whitespace-pre-line leading-relaxed">
                {equipment.specifications || 'Manufacturer baseline specifications apply.'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Interactive Calendar Section */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 sm:p-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-6 border-b border-slate-200 gap-3">
          <div>
            <h2 className="text-xl font-bold text-slate-900 tracking-tight flex items-center space-x-2">
              <Calendar className="w-5 h-5 text-teal-700" />
              <span>Availability & Schedule Calendar</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Select or click on any time window to request a reservation.
            </p>
          </div>

          <div className="flex items-center space-x-3 text-xs">
            <span className="flex items-center gap-1.5 font-medium text-slate-700">
              <span className="w-3 h-3 rounded-full bg-emerald-500"></span> Confirmed
            </span>
            <span className="flex items-center gap-1.5 font-medium text-slate-700">
              <span className="w-3 h-3 rounded-full bg-amber-500"></span> Pending Review
            </span>
            <span className="flex items-center gap-1.5 font-medium text-slate-700">
              <span className="w-3 h-3 rounded-full bg-blue-500"></span> In Use
            </span>
          </div>
        </div>

        <div className="mt-6">
          <FullCalendar
            plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
            initialView="timeGridWeek"
            headerToolbar={{
              left: 'prev,next today',
              center: 'title',
              right: 'dayGridMonth,timeGridWeek,timeGridDay',
            }}
            selectable={isAuthenticated && isBookable}
            selectMirror={true}
            dayMaxEvents={true}
            weekends={true}
            events={events}
            select={handleDateSelect}
            height="auto"
            slotMinTime="08:00:00"
            slotMaxTime="20:00:00"
          />
        </div>
      </div>

      {/* Booking Form Modal */}
      {showBookingModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200">
            <div className="flex justify-between items-center pb-4 border-b border-slate-200">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Reserve Equipment</h3>
                <p className="text-xs text-slate-500">{equipment.name}</p>
              </div>
              <button onClick={() => setShowBookingModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {bookingSuccess && (
              <div className="my-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
                <span>{bookingSuccess}</span>
              </div>
            )}

            {bookingError && (
              <div className="my-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg flex items-center space-x-2">
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                <span>{bookingError}</span>
              </div>
            )}

            <form onSubmit={handleBookingSubmit} className="mt-4 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-700">Start Date & Time *</label>
                  <input
                    type="datetime-local"
                    required
                    value={bookingForm.startTime}
                    onChange={(e) => setBookingForm({ ...bookingForm, startTime: e.target.value })}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">End Date & Time *</label>
                  <input
                    type="datetime-local"
                    required
                    value={bookingForm.endTime}
                    onChange={(e) => setBookingForm({ ...bookingForm, endTime: e.target.value })}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700">Research Purpose / Experiment Summary *</label>
                <textarea
                  rows={3}
                  required
                  value={bookingForm.purpose}
                  onChange={(e) => setBookingForm({ ...bookingForm, purpose: e.target.value })}
                  placeholder="Outline your research hypothesis, protocol, or sample preparation..."
                  className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700">Special Requirements / Notes (Optional)</label>
                <input
                  type="text"
                  value={bookingForm.notes}
                  onChange={(e) => setBookingForm({ ...bookingForm, notes: e.target.value })}
                  placeholder="e.g. Cryo holder needed, specific laser line, sample cooling..."
                  className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowBookingModal(false)}
                  className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-xs hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={bookingLoading}
                  className="px-4 py-2 bg-teal-700 text-white rounded-lg text-xs hover:bg-teal-800 disabled:opacity-50 font-semibold"
                >
                  {bookingLoading ? 'Checking Availability...' : 'Confirm Reservation Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
