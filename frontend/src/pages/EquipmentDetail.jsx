import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { EquipmentStatusBadge } from '../components/common/StatusBadge';
import { canManageSharing, isSystemAdmin, canBookEquipment, normalizeRole } from '../utils/roleHelper';
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
  ArrowLeft,
  ListOrdered,
  Sparkles,
  Repeat,
  ArrowRight,
  TrendingDown,
  Info
} from 'lucide-react';
import { format } from 'date-fns';

export default function EquipmentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
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
  const [isConflict, setIsConflict] = useState(false);

  // Suggestions State (Task T4)
  const [suggestions, setSuggestions] = useState(null);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);

  // Waitlist Join State
  const [showWaitlistModal, setShowWaitlistModal] = useState(false);
  const [waitlistNotes, setWaitlistNotes] = useState('');
  const [waitlistLoading, setWaitlistLoading] = useState(false);
  const [waitlistSuccess, setWaitlistSuccess] = useState('');
  const [waitlistError, setWaitlistError] = useState('');

  // Recurring Booking Form State (Task T4)
  const [isRecurring, setIsRecurring] = useState(false);
  const [recurrenceForm, setRecurrenceForm] = useState({
    frequency: 'WEEKLY',
    interval: 1,
    weekdays: [1], // Monday
    untilDate: '',
  });
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewResult, setPreviewResult] = useState(null);

  // Inter-Institution Sharing Listing State (Task T5, T11)
  const [sharingListing, setSharingListing] = useState(null);
  const [showShareModal, setShowShareModal] = useState(false);
  const [shareLoading, setShareLoading] = useState(false);
  const [shareSuccess, setShareSuccess] = useState('');
  const [shareError, setShareError] = useState('');
  const [shareForm, setShareForm] = useState({
    feePerHour: '',
    externalSharePercentage: 20,
    minNoticeHours: 24,
    maxBookingHours: 4,
    termsAndConditions: '',
    active: true,
  });

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
          if (res.data.hourlyRate && !shareForm.feePerHour) {
            setShareForm((prev) => ({ ...prev, feePerHour: res.data.hourlyRate }));
          }
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

  const fetchSharingListing = () => {
    api.get(`/sharing/listings/equipment/${id}`)
      .then((res) => {
        if (res.success && res.data) {
          setSharingListing(res.data);
          setShareForm({
            feePerHour: res.data.feePerHour,
            externalSharePercentage: res.data.externalSharePercentage,
            minNoticeHours: res.data.minNoticeHours,
            maxBookingHours: res.data.maxBookingHours,
            termsAndConditions: res.data.termsAndConditions || '',
            active: res.data.active,
          });
        }
      })
      .catch((err) => console.warn('No active sharing listing found', err));
  };

  useEffect(() => {
    fetchEquipmentDetails();
    fetchCalendarEvents();
    fetchSharingListing();
  }, [id]);

  const handleSaveSharingListing = async (e) => {
    e.preventDefault();
    setShareLoading(true);
    setShareError('');
    setShareSuccess('');

    try {
      const res = await api.post('/sharing/listings', {
        equipmentId: Number(id),
        feePerHour: Number(shareForm.feePerHour) || 0,
        externalSharePercentage: Number(shareForm.externalSharePercentage) || 20,
        minNoticeHours: Number(shareForm.minNoticeHours) || 24,
        maxBookingHours: Number(shareForm.maxBookingHours) || 4,
        termsAndConditions: shareForm.termsAndConditions,
        active: shareForm.active,
      });

      if (res.success) {
        setSharingListing(res.data);
        setShareSuccess('Inter-institution equipment sharing parameters updated successfully!');
        setTimeout(() => {
          setShowShareModal(false);
          setShareSuccess('');
        }, 2000);
      }
    } catch (err) {
      setShareError(err.message || 'Failed to update sharing listing.');
    } finally {
      setShareLoading(false);
    }
  };

  const handleDateSelect = (selectInfo) => {
    if (!isAuthenticated) return;
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
    setIsConflict(false);
    setSuggestions(null);
    setIsRecurring(false);
    setPreviewResult(null);
    setShowBookingModal(true);
  };

  const getLocalIso = (date) => {
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  };

  const setDemoNow = () => {
    const now = new Date();
    const start = new Date(now.getTime() + 2 * 60 * 1000);
    const end = new Date(now.getTime() + 62 * 60 * 1000);
    setBookingForm((prev) => ({
      ...prev,
      startTime: getLocalIso(start),
      endTime: getLocalIso(end),
    }));
    setIsConflict(false);
  };

  const setDemoTomorrow = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(10, 0, 0, 0);
    const end = new Date(tomorrow);
    end.setHours(12, 0, 0, 0);
    setBookingForm((prev) => ({
      ...prev,
      startTime: getLocalIso(tomorrow),
      endTime: getLocalIso(end),
    }));
    setIsConflict(false);
  };

  const handleOpenBookingModal = () => {
    const now = new Date();
    const start = new Date(now.getTime() + 2 * 60 * 1000);
    const end = new Date(now.getTime() + 62 * 60 * 1000);

    const until = new Date(now);
    until.setDate(until.getDate() + 28);
    const untilStr = until.toISOString().slice(0, 10);

    setBookingForm({
      startTime: getLocalIso(start),
      endTime: getLocalIso(end),
      purpose: '',
      notes: '',
    });
    setRecurrenceForm({
      frequency: 'WEEKLY',
      interval: 1,
      weekdays: [start.getDay() === 0 ? 7 : start.getDay()],
      untilDate: untilStr,
    });
    setBookingError('');
    setBookingSuccess('');
    setIsConflict(false);
    setSuggestions(null);
    setIsRecurring(false);
    setPreviewResult(null);
    setShowBookingModal(true);
  };

  const loadBookingSuggestions = async (startTime, endTime) => {
    setLoadingSuggestions(true);
    try {
      const res = await api.get('/bookings/suggestions', {
        params: {
          equipmentId: id,
          startTime: startTime,
          endTime: endTime,
        },
      });
      if (res.success && res.data) {
        setSuggestions(res.data);
      }
    } catch (e) {
      console.warn('Failed to fetch booking suggestions:', e);
    } finally {
      setLoadingSuggestions(false);
    }
  };

  const handlePreviewRecurring = async () => {
    setBookingError('');
    setPreviewLoading(true);
    try {
      const payload = {
        equipmentId: Number(id),
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        frequency: recurrenceForm.frequency,
        interval: Number(recurrenceForm.interval) || 1,
        weekdays: recurrenceForm.frequency === 'WEEKLY' ? recurrenceForm.weekdays : null,
        untilDate: recurrenceForm.untilDate,
        purpose: bookingForm.purpose || 'Recurring Research Session',
        notes: bookingForm.notes || '',
      };
      const res = await api.post('/bookings/recurring/preview', payload);
      if (res.success) {
        setPreviewResult(res.data);
      }
    } catch (err) {
      setBookingError(err.message || 'Failed to preview recurring occurrences.');
    } finally {
      setPreviewLoading(false);
    }
  };

  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    setBookingError('');
    setBookingSuccess('');
    setIsConflict(false);
    setBookingLoading(true);

    try {
      if (isRecurring) {
        const payload = {
          equipmentId: Number(id),
          startTime: bookingForm.startTime,
          endTime: bookingForm.endTime,
          frequency: recurrenceForm.frequency,
          interval: Number(recurrenceForm.interval) || 1,
          weekdays: recurrenceForm.frequency === 'WEEKLY' ? recurrenceForm.weekdays : null,
          untilDate: recurrenceForm.untilDate,
          purpose: bookingForm.purpose,
          notes: bookingForm.notes,
        };
        const res = await api.post('/bookings/recurring', payload);
        if (res.success) {
          setBookingSuccess(`Created recurring series with ${res.data?.length || 'all'} confirmed/requested sessions!`);
          fetchCalendarEvents();
          setTimeout(() => {
            setShowBookingModal(false);
            setBookingSuccess('');
          }, 2500);
        }
      } else {
        const res = await api.post('/bookings', {
          equipmentId: Number(id),
          startTime: bookingForm.startTime,
          endTime: bookingForm.endTime,
          purpose: bookingForm.purpose,
          notes: bookingForm.notes,
        });

        if (res.success) {
          setBookingSuccess('Booking reservation submitted successfully!');
          fetchCalendarEvents();
          setTimeout(() => {
            setShowBookingModal(false);
            setBookingSuccess('');
          }, 2000);
        }
      }
    } catch (err) {
      setBookingError(err.message || 'Failed to submit booking reservation.');
      if (err.status === 409 || (err.message && err.message.toLowerCase().includes('conflict'))) {
        setIsConflict(true);
        loadBookingSuggestions(bookingForm.startTime, bookingForm.endTime);
      }
    } finally {
      setBookingLoading(false);
    }
  };

  const handleJoinWaitlist = async (e) => {
    e.preventDefault();
    setWaitlistError('');
    setWaitlistSuccess('');
    setWaitlistLoading(true);

    try {
      const res = await api.post('/waitlist', {
        equipmentId: Number(id),
        requestedStartTime: bookingForm.startTime,
        requestedEndTime: bookingForm.endTime,
        notes: waitlistNotes || 'Joined from equipment detail schedule conflict',
      });

      if (res.success) {
        setWaitlistSuccess(`You joined the waitlist at Queue Position #${res.data.queuePosition}! You will receive an offer if this slot is freed.`);
        setTimeout(() => {
          setShowWaitlistModal(false);
          setWaitlistSuccess('');
        }, 3000);
      }
    } catch (err) {
      setWaitlistError(err.message || 'Failed to join waitlist.');
    } finally {
      setWaitlistLoading(false);
    }
  };

  const applySlotSuggestion = (suggestedStart, suggestedEnd, targetEquipmentId) => {
    if (targetEquipmentId && Number(targetEquipmentId) !== Number(id)) {
      navigate(`/equipment/${targetEquipmentId}`);
      return;
    }
    setBookingForm((prev) => ({
      ...prev,
      startTime: suggestedStart.slice(0, 16),
      endTime: suggestedEnd.slice(0, 16),
    }));
    setIsConflict(false);
    setBookingError('');
    setSuggestions(null);
  };

  const toggleWeekday = (day) => {
    setRecurrenceForm((prev) => {
      const exists = prev.weekdays.includes(day);
      let updated;
      if (exists) {
        updated = prev.weekdays.filter((d) => d !== day);
        if (updated.length === 0) updated = [day]; // keep at least one
      } else {
        updated = [...prev.weekdays, day].sort();
      }
      return { ...prev, weekdays: updated };
    });
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

  const isExternalUser = isAuthenticated && equipment && user?.department?.institution?.id &&
    (equipment.institution?.id || equipment.department?.institution?.id) &&
    user.department.institution.id !== (equipment.institution?.id || equipment.department?.institution?.id);

  const canManageThisSharing = isAuthenticated && (
    isSystemAdmin(user) || (canManageSharing(user) && (
      user?.department?.institution?.id === equipment.institution?.id ||
      user?.department?.institution?.id === equipment.department?.institution?.id
    ))
  );

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
            <div className="bg-slate-50 p-6 rounded-2xl border border-slate-200 min-w-[280px] text-center lg:text-right space-y-4">
              <div>
                <span className="text-xs text-slate-500 block uppercase font-medium">Standard Usage Rate</span>
                <span className="text-3xl font-extrabold text-slate-900">${equipment.hourlyRate}</span>
                <span className="text-xs text-slate-500"> / hour</span>
              </div>

              {isAuthenticated ? (
                <div className="space-y-2">
                  {canBookEquipment(user) ? (
                    <>
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

                      <button
                        onClick={() => {
                          setShowWaitlistModal(true);
                          setWaitlistError('');
                          setWaitlistSuccess('');
                        }}
                        className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold border border-teal-700 text-teal-800 hover:bg-teal-50 transition flex items-center justify-center space-x-1.5"
                      >
                        <ListOrdered className="w-4 h-4 text-teal-700" />
                        <span>Join Slot Waitlist</span>
                      </button>
                    </>
                  ) : (
                    <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-left text-xs text-amber-900 space-y-1">
                      <p className="font-semibold flex items-center">
                        <Info className="w-3.5 h-3.5 mr-1 text-amber-700 flex-shrink-0" />
                        Staff / Administrative Role
                      </p>
                      <p className="text-slate-600">
                        You are logged in as <span className="font-semibold text-slate-800">{normalizeRole(user?.role).replace('_', ' ')}</span>. Equipment reservations &amp; waitlists are reserved for <strong>Researcher/Student</strong> and <strong>Department Head</strong> accounts.
                      </p>
                    </div>
                  )}

                  {equipment.sharableInterInstitution && canManageThisSharing && (
                    <button
                      onClick={() => {
                        setShowShareModal(true);
                        setShareError('');
                        setShareSuccess('');
                      }}
                      className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold border border-indigo-300 text-indigo-700 hover:bg-indigo-50 transition flex items-center justify-center space-x-1.5"
                    >
                      <Share2 className="w-3.5 h-3.5 text-indigo-600" />
                      <span>Configure Inter-Institution Sharing</span>
                    </button>
                  )}
                </div>
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

          {/* Inter-Institution Sharing Status Banner */}
          {sharingListing && sharingListing.active && (
            <div className="mt-6 p-4 bg-gradient-to-r from-teal-50 to-indigo-50 rounded-2xl border border-teal-200/80 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-teal-900 flex items-center gap-1.5">
                  <Share2 className="w-4 h-4 text-teal-700" />
                  Inter-Institution Sharing Active
                </span>
                <span className="text-xs font-extrabold text-indigo-900 bg-white px-2.5 py-0.5 rounded-md border border-indigo-100">
                  ${sharingListing.feePerHour}/hr
                </span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs text-slate-700">
                <div><span className="text-slate-500 font-medium">Notice Required:</span> <strong className="text-slate-800">{sharingListing.minNoticeHours} hours</strong></div>
                <div><span className="text-slate-500 font-medium">Max Session:</span> <strong className="text-slate-800">{sharingListing.maxBookingHours} hours</strong></div>
                <div><span className="text-slate-500 font-medium">External Capacity:</span> <strong className="text-teal-800">{sharingListing.externalSharePercentage}% / week</strong></div>
              </div>
              {sharingListing.termsAndConditions && (
                <p className="text-[11px] text-slate-600 italic pt-1 border-t border-teal-200/60">
                  Terms: {sharingListing.termsAndConditions}
                </p>
              )}
            </div>
          )}
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
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-xl border border-slate-200 max-h-[92vh] overflow-y-auto">
            <div className="flex justify-between items-center pb-4 border-b border-slate-200">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Reserve Equipment</h3>
                <p className="text-xs text-slate-500">{equipment.name} • ${equipment.hourlyRate}/hr</p>
              </div>
              <button onClick={() => setShowBookingModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {isExternalUser && (
              <div className="my-3 p-3 bg-indigo-50 border border-indigo-200 rounded-xl text-xs text-indigo-900 space-y-1">
                <div className="flex items-center gap-1.5 font-bold">
                  <Share2 className="w-3.5 h-3.5 text-indigo-700" />
                  Inter-Institution External Reservation
                </div>
                <p className="text-[11px] text-indigo-800">
                  This equipment belongs to <strong>{equipment.institution?.name || equipment.department?.institution?.name}</strong>. Your reservation will be billed to your institution under an active bilateral agreement at <strong>${sharingListing?.feePerHour || equipment.hourlyRate}/hour</strong>.
                </p>
              </div>
            )}

            {bookingSuccess && (
              <div className="my-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
                <span>{bookingSuccess}</span>
              </div>
            )}

            {bookingError && (
              <div className="my-4 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg space-y-2">
                <div className="flex items-center space-x-2">
                  <AlertCircle className="w-5 h-5 flex-shrink-0" />
                  <span className="font-medium">{bookingError}</span>
                </div>
                {isConflict && (
                  <div className="pt-2 border-t border-rose-200 flex items-center justify-between">
                    <span className="text-[11px] text-rose-700">Time window has an active booking or hold.</span>
                    <button
                      type="button"
                      onClick={() => {
                        setShowBookingModal(false);
                        setShowWaitlistModal(true);
                      }}
                      className="px-2.5 py-1 bg-rose-600 hover:bg-rose-700 text-white rounded-md text-xs font-semibold"
                    >
                      Join Waitlist Instead
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Conflict Suggestions Drawer (Task T4) */}
            {isConflict && suggestions && (
              <div className="mb-4 p-4 bg-teal-50/70 border border-teal-200 rounded-xl space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-teal-900 flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4 text-teal-700" />
                    Intelligent Scheduling Recommendations
                  </span>
                  {loadingSuggestions && <span className="text-[11px] text-teal-700 animate-pulse">Finding slots...</span>}
                </div>

                {/* Rule 1: Adjacent Slots */}
                {suggestions.nearestSlotsOnSameEquipment?.length > 0 && (
                  <div className="space-y-1.5">
                    <span className="text-[11px] font-semibold text-slate-700 uppercase tracking-wider block">
                      Closest Available Slots (Same Equipment)
                    </span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {suggestions.nearestSlotsOnSameEquipment.map((slot, idx) => (
                        <div key={idx} className="p-2 bg-white rounded-lg border border-teal-100 flex items-center justify-between text-xs">
                          <div>
                            <span className="font-semibold text-slate-800 block">{format(new Date(slot.startTime), 'EEE, MMM d')}</span>
                            <span className="text-slate-500 text-[11px]">{format(new Date(slot.startTime), 'h:mm a')} – {format(new Date(slot.endTime), 'h:mm a')}</span>
                          </div>
                          <button
                            type="button"
                            onClick={() => applySlotSuggestion(slot.startTime, slot.endTime)}
                            className="px-2 py-1 bg-teal-700 text-white rounded text-[11px] font-medium hover:bg-teal-800"
                          >
                            Use Slot
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Rule 2: Alternative Equipment in same category */}
                {suggestions.alternativeEquipmentSuggestions?.length > 0 && (
                  <div className="space-y-1.5 pt-2 border-t border-teal-200">
                    <span className="text-[11px] font-semibold text-slate-700 uppercase tracking-wider block">
                      Alternative Equipment (Same Category & Lower 28-day Utilization)
                    </span>
                    <div className="space-y-1.5">
                      {suggestions.alternativeEquipmentSuggestions.map((alt) => (
                        <div key={alt.equipmentId} className="p-2.5 bg-white rounded-lg border border-teal-100 flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-slate-900">{alt.equipmentName}</span>
                            <div className="text-[11px] text-slate-500 flex items-center gap-2 mt-0.5">
                              <span>{alt.departmentName}</span>
                              <span>•</span>
                              <span className="text-teal-700 font-semibold">{alt.past28DayUtilizationRate}% utilized</span>
                            </div>
                          </div>
                          <button
                            type="button"
                            onClick={() => applySlotSuggestion(alt.suggestedStartTime, alt.suggestedEndTime, alt.equipmentId)}
                            className="px-2.5 py-1 bg-slate-800 text-white rounded text-[11px] font-medium hover:bg-slate-900 flex items-center gap-1"
                          >
                            <span>Switch</span>
                            <ArrowRight className="w-3 h-3" />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Rule 3: Quiet / Off-Peak Slots */}
                {suggestions.quietOffPeakSlots?.length > 0 && (
                  <div className="space-y-1.5 pt-2 border-t border-teal-200">
                    <span className="text-[11px] font-semibold text-slate-700 uppercase tracking-wider block">
                      Low-Traffic Off-Peak Windows
                    </span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {suggestions.quietOffPeakSlots.map((slot, idx) => (
                        <div key={idx} className="p-2 bg-white rounded-lg border border-teal-100 flex items-center justify-between text-xs">
                          <div>
                            <div className="flex items-center gap-1">
                              <span className="font-semibold text-slate-800">{format(new Date(slot.startTime), 'EEE, MMM d')}</span>
                              <span className="px-1.5 py-0.2 bg-emerald-100 text-emerald-800 text-[9px] font-bold rounded">Usually Quiet</span>
                            </div>
                            <span className="text-slate-500 text-[11px]">{format(new Date(slot.startTime), 'h:mm a')} – {format(new Date(slot.endTime), 'h:mm a')}</span>
                          </div>
                          <button
                            type="button"
                            onClick={() => applySlotSuggestion(slot.startTime, slot.endTime)}
                            className="px-2 py-1 bg-emerald-700 text-white rounded text-[11px] font-medium hover:bg-emerald-800"
                          >
                            Use Slot
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            <form onSubmit={handleBookingSubmit} className="mt-4 space-y-4">
              {/* Quick Time Presets for Demo */}
              <div className="flex items-center justify-between bg-slate-50 p-2.5 rounded-lg border border-slate-200">
                <span className="text-[11px] font-medium text-slate-600">Quick Presets:</span>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={setDemoNow}
                    className="inline-flex items-center px-2.5 py-1 bg-teal-700 hover:bg-teal-800 text-white rounded text-[11px] font-semibold transition"
                    title="Start 2 minutes from now to immediately test Check-In in My Bookings"
                  >
                    ⚡ Start Now (Live Check-In Demo)
                  </button>
                  <button
                    type="button"
                    onClick={setDemoTomorrow}
                    className="inline-flex items-center px-2.5 py-1 bg-white hover:bg-slate-100 border border-slate-300 text-slate-700 rounded text-[11px] font-medium transition"
                  >
                    Tomorrow 10 AM
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-700">Start Date & Time *</label>
                  <input
                    type="datetime-local"
                    required
                    value={bookingForm.startTime}
                    onChange={(e) => {
                      setBookingForm({ ...bookingForm, startTime: e.target.value });
                      setIsConflict(false);
                    }}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">End Date & Time *</label>
                  <input
                    type="datetime-local"
                    required
                    value={bookingForm.endTime}
                    onChange={(e) => {
                      setBookingForm({ ...bookingForm, endTime: e.target.value });
                      setIsConflict(false);
                    }}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700">Research Purpose / Experiment Summary *</label>
                <textarea
                  rows={2}
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

              {/* Recurring Booking Toggle (Task T4) */}
              <div className="pt-2 border-t border-slate-200">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isRecurring}
                    onChange={(e) => {
                      setIsRecurring(e.target.checked);
                      setPreviewResult(null);
                    }}
                    className="w-4 h-4 text-teal-600 rounded border-slate-300 focus:ring-teal-500"
                  />
                  <span className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                    <Repeat className="w-3.5 h-3.5 text-teal-700" />
                    Repeat this reservation (Recurring Series)
                  </span>
                </label>

                {isRecurring && (
                  <div className="mt-3 p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                      <div>
                        <label className="block text-[11px] font-medium text-slate-600">Frequency</label>
                        <select
                          value={recurrenceForm.frequency}
                          onChange={(e) => setRecurrenceForm({ ...recurrenceForm, frequency: e.target.value })}
                          className="mt-1 w-full p-1.5 bg-white border border-slate-300 rounded text-xs"
                        >
                          <option value="WEEKLY">Weekly</option>
                          <option value="DAILY">Daily</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-[11px] font-medium text-slate-600">Repeat Every</label>
                        <div className="mt-1 flex items-center space-x-1">
                          <input
                            type="number"
                            min="1"
                            max="8"
                            value={recurrenceForm.interval}
                            onChange={(e) => setRecurrenceForm({ ...recurrenceForm, interval: Number(e.target.value) })}
                            className="w-16 p-1.5 bg-white border border-slate-300 rounded text-xs"
                          />
                          <span className="text-[11px] text-slate-500">
                            {recurrenceForm.frequency === 'DAILY' ? 'day(s)' : 'week(s)'}
                          </span>
                        </div>
                      </div>

                      <div>
                        <label className="block text-[11px] font-medium text-slate-600">Repeat Until *</label>
                        <input
                          type="date"
                          required
                          value={recurrenceForm.untilDate}
                          onChange={(e) => setRecurrenceForm({ ...recurrenceForm, untilDate: e.target.value })}
                          className="mt-1 w-full p-1.5 bg-white border border-slate-300 rounded text-xs"
                        />
                      </div>
                    </div>

                    {recurrenceForm.frequency === 'WEEKLY' && (
                      <div>
                        <label className="block text-[11px] font-medium text-slate-600 mb-1.5">Days of the Week</label>
                        <div className="flex flex-wrap gap-1.5">
                          {[
                            { day: 1, label: 'Mon' },
                            { day: 2, label: 'Tue' },
                            { day: 3, label: 'Wed' },
                            { day: 4, label: 'Thu' },
                            { day: 5, label: 'Fri' },
                            { day: 6, label: 'Sat' },
                            { day: 7, label: 'Sun' },
                          ].map(({ day, label }) => {
                            const isSelected = recurrenceForm.weekdays.includes(day);
                            return (
                              <button
                                key={day}
                                type="button"
                                onClick={() => toggleWeekday(day)}
                                className={`px-2.5 py-1 text-[11px] font-semibold rounded-md transition ${
                                  isSelected
                                    ? 'bg-teal-700 text-white'
                                    : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-100'
                                }`}
                              >
                                {label}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    <div className="pt-2 flex items-center justify-between">
                      <button
                        type="button"
                        onClick={handlePreviewRecurring}
                        disabled={previewLoading || !recurrenceForm.untilDate}
                        className="px-3 py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg text-xs font-medium disabled:opacity-50"
                      >
                        {previewLoading ? 'Checking Series...' : 'Preview Occurrences & Conflicts'}
                      </button>
                      {previewResult && (
                        <div className="text-xs">
                          <span className="font-semibold text-slate-800">{previewResult.totalOccurrences} dates</span>
                          {' • '}
                          <span className="text-emerald-700 font-semibold">{previewResult.availableCount} available</span>
                          {previewResult.conflictCount > 0 && (
                            <>
                              {' • '}
                              <span className="text-rose-700 font-semibold">{previewResult.conflictCount} conflict(s)</span>
                            </>
                          )}
                        </div>
                      )}
                    </div>

                    {previewResult && previewResult.conflicts?.length > 0 && (
                      <div className="p-2.5 bg-rose-50 border border-rose-200 rounded-lg text-xs space-y-1">
                        <span className="font-bold text-rose-800 block">Conflict Warning:</span>
                        <div className="max-h-24 overflow-y-auto space-y-0.5 text-[11px] text-rose-700">
                          {previewResult.conflicts.map((c, i) => (
                            <div key={i}>
                              • {format(new Date(c.startTime), 'EEE, MMM d, yyyy h:mm a')} – {c.reason}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
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
                  {bookingLoading
                    ? 'Processing...'
                    : isRecurring
                    ? 'Submit Recurring Series'
                    : 'Confirm Reservation Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Configure Sharing Listing Modal (Task T5, T11) */}
      {showShareModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200">
            <div className="flex justify-between items-center pb-3 border-b border-slate-200">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Share2 className="w-4 h-4 text-indigo-700" />
                Configure Inter-Institution Equipment Sharing
              </h3>
              <button onClick={() => setShowShareModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {shareSuccess && (
              <div className="my-3 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-xl flex items-center space-x-2">
                <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
                <span>{shareSuccess}</span>
              </div>
            )}

            {shareError && (
              <div className="my-3 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-xl flex items-center space-x-2">
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                <span>{shareError}</span>
              </div>
            )}

            <form onSubmit={handleSaveSharingListing} className="mt-4 space-y-4">
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs space-y-1">
                <div>
                  <span className="text-slate-500 font-medium">Equipment:</span>{' '}
                  <span className="font-bold text-slate-900">{equipment.name}</span>
                </div>
                <div>
                  <span className="text-slate-500 font-medium">Internal Rate:</span>{' '}
                  <span className="text-slate-800">${equipment.hourlyRate}/hr</span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    External Rate ($ / hr) *
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={shareForm.feePerHour}
                    onChange={(e) => setShareForm({ ...shareForm, feePerHour: e.target.value })}
                    className="w-full p-2 border border-slate-300 rounded-xl text-xs focus:ring-teal-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    External Share Limit (%) *
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="100"
                    required
                    value={shareForm.externalSharePercentage}
                    onChange={(e) => setShareForm({ ...shareForm, externalSharePercentage: e.target.value })}
                    className="w-full p-2 border border-slate-300 rounded-xl text-xs focus:ring-teal-600"
                  />
                  <span className="text-[10px] text-slate-400 block mt-0.5">Max % weekly capacity</span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Minimum Advance Notice (Hours) *
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="720"
                    required
                    value={shareForm.minNoticeHours}
                    onChange={(e) => setShareForm({ ...shareForm, minNoticeHours: e.target.value })}
                    className="w-full p-2 border border-slate-300 rounded-xl text-xs focus:ring-teal-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Max Booking Duration (Hours) *
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="24"
                    required
                    value={shareForm.maxBookingHours}
                    onChange={(e) => setShareForm({ ...shareForm, maxBookingHours: e.target.value })}
                    className="w-full p-2 border border-slate-300 rounded-xl text-xs focus:ring-teal-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Access Requirements &amp; Terms
                </label>
                <textarea
                  rows={2}
                  value={shareForm.termsAndConditions}
                  onChange={(e) => setShareForm({ ...shareForm, termsAndConditions: e.target.value })}
                  placeholder="e.g. Mandatory lab safety orientation; bring own cryo-gloves."
                  className="w-full p-2.5 border border-slate-300 rounded-xl text-xs"
                />
              </div>

              <div className="pt-2">
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={shareForm.active}
                    onChange={(e) => setShareForm({ ...shareForm, active: e.target.checked })}
                    className="w-4 h-4 text-teal-600 rounded border-slate-300 focus:ring-teal-500"
                  />
                  <span className="text-xs font-semibold text-slate-800">
                    Publish this equipment for inter-institution sharing discovery
                  </span>
                </label>
              </div>

              <div className="flex justify-end space-x-2 pt-3 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowShareModal(false)}
                  className="px-3.5 py-1.5 border border-slate-300 text-slate-700 rounded-xl text-xs hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={shareLoading}
                  className="px-4 py-1.5 bg-indigo-700 text-white rounded-xl text-xs font-semibold hover:bg-indigo-800 disabled:opacity-50"
                >
                  {shareLoading ? 'Saving...' : 'Save Parameters'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
