import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { EquipmentStatusBadge } from '../components/common/StatusBadge';
import { Search, Filter, Plus, Beaker, MapPin, DollarSign, Share2, Layers, AlertCircle, X } from 'lucide-react';

export default function EquipmentCatalog() {
  const { user } = useAuth();

  const [equipmentList, setEquipmentList] = useState([]);
  const [categories, setCategories] = useState([]);
  const [institutions, setInstitutions] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Filters state
  const [keyword, setKeyword] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedInstitution, setSelectedInstitution] = useState('');

  // Add Equipment Modal State
  const [showAddModal, setShowAddModal] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState('');
  const [newEquip, setNewEquip] = useState({
    name: '',
    modelNumber: '',
    serialNumber: '',
    category: '',
    tags: '',
    description: '',
    specifications: '',
    locationRoom: '',
    hourlyRate: 50,
    sharableInterInstitution: true,
    institutionId: '',
    departmentId: '',
  });

  // Inline Quick-Add Institution/Department
  const [showQuickInst, setShowQuickInst] = useState(false);
  const [quickInstName, setQuickInstName] = useState('');
  const [showQuickDept, setShowQuickDept] = useState(false);
  const [quickDeptName, setQuickDeptName] = useState('');

  const canAddEquipment = user && [
    'ROLE_LAB_MANAGER',
    'ROLE_LAB_TECHNICIAN',
    'ROLE_DEPARTMENT_HEAD',
    'ROLE_INSTITUTION_ADMIN',
    'ROLE_SYSTEM_ADMIN',
  ].includes(user.role);

  const fetchEquipment = () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (keyword) params.append('keyword', keyword);
    if (selectedCategory) params.append('category', selectedCategory);
    if (selectedStatus) params.append('status', selectedStatus);
    if (selectedInstitution) params.append('institutionId', selectedInstitution);

    api.get(`/equipment?${params.toString()}`)
      .then((res) => {
        if (res.success && res.data) {
          setEquipmentList(res.data);
        }
      })
      .catch((err) => setError(err.message || 'Failed to load equipment catalog.'))
      .finally(() => setLoading(false));
  };

  const loadInstitutions = () => {
    api.get('/institutions')
      .then((res) => {
        if (res.success && res.data) {
          setInstitutions(res.data);
          if (res.data.length > 0 && !newEquip.institutionId) {
            setNewEquip((prev) => ({ ...prev, institutionId: res.data[0].id }));
          }
        }
      })
      .catch((err) => console.error(err));
  };

  useEffect(() => {
    fetchEquipment();
  }, [keyword, selectedCategory, selectedStatus, selectedInstitution]);

  useEffect(() => {
    api.get('/equipment/categories')
      .then((res) => res.success && setCategories(res.data || []))
      .catch((err) => console.error(err));

    loadInstitutions();
  }, []);

  useEffect(() => {
    if (newEquip.institutionId) {
      api.get(`/institutions/${newEquip.institutionId}/departments`)
        .then((res) => {
          if (res.success && res.data) {
            setDepartments(res.data);
            if (res.data.length > 0) {
              setNewEquip((prev) => ({ ...prev, departmentId: res.data[0].id }));
            } else {
              setNewEquip((prev) => ({ ...prev, departmentId: '' }));
            }
          }
        })
        .catch((err) => console.error(err));
    } else {
      setDepartments([]);
    }
  }, [newEquip.institutionId]);

  const handleQuickAddInstitution = async () => {
    if (!quickInstName.trim()) return;
    try {
      const res = await api.post('/institutions', { name: quickInstName.trim() });
      if (res.success && res.data) {
        setInstitutions((prev) => [...prev, res.data]);
        setNewEquip((prev) => ({ ...prev, institutionId: res.data.id }));
        setQuickInstName('');
        setShowQuickInst(false);
      }
    } catch (err) {
      alert(err.message || 'Failed to add institution.');
    }
  };

  const handleQuickAddDepartment = async () => {
    if (!quickDeptName.trim() || !newEquip.institutionId) return;
    try {
      const res = await api.post(`/institutions/${newEquip.institutionId}/departments`, { name: quickDeptName.trim() });
      if (res.success && res.data) {
        setDepartments((prev) => [...prev, res.data]);
        setNewEquip((prev) => ({ ...prev, departmentId: res.data.id }));
        setQuickDeptName('');
        setShowQuickDept(false);
      }
    } catch (err) {
      alert(err.message || 'Failed to add department.');
    }
  };

  const handleCreateEquipment = async (e) => {
    e.preventDefault();
    setModalError('');

    if (!newEquip.institutionId) {
      setModalError('Please create or select an Institution first.');
      return;
    }
    if (!newEquip.departmentId) {
      setModalError('Please create or select a Department first.');
      return;
    }

    setModalLoading(true);

    try {
      const res = await api.post('/equipment', {
        ...newEquip,
        institutionId: Number(newEquip.institutionId),
        departmentId: Number(newEquip.departmentId),
        hourlyRate: Number(newEquip.hourlyRate),
      });

      if (res.success) {
        setShowAddModal(false);
        fetchEquipment();
        // Refresh categories
        api.get('/equipment/categories').then((r) => r.success && setCategories(r.data || []));
      }
    } catch (err) {
      setModalError(err.message || 'Failed to register equipment.');
    } finally {
      setModalLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between pb-6 border-b border-slate-200 gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Laboratory Equipment Catalog
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Discover, check real-time availability, and reserve high-value research instrumentation across institutions.
          </p>
        </div>

        {canAddEquipment && (
          <button
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center px-4 py-2.5 bg-teal-700 hover:bg-teal-800 text-white text-sm font-medium rounded-xl shadow-sm transition"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Register Equipment
          </button>
        )}
      </div>

      {/* Filter & Search Bar */}
      <div className="my-6 bg-white p-4 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Keyword Search */}
          <div className="md:col-span-1 relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="h-4 w-4" />
            </div>
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Search name, model, specs..."
              className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-teal-600"
            />
          </div>

          {/* Category Filter */}
          <div>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="">All Categories</option>
              {categories.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="">All Statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="BOOKED">Booked</option>
              <option value="UNDER_MAINTENANCE">Under Maintenance</option>
              <option value="OUT_OF_SERVICE">Out of Service</option>
            </select>
          </div>

          {/* Institution Filter */}
          <div>
            <select
              value={selectedInstitution}
              onChange={(e) => setSelectedInstitution(e.target.value)}
              className="w-full py-2 px-3 border border-slate-300 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-600"
            >
              <option value="">All Institutions</option>
              {institutions.map((inst) => (
                <option key={inst.id} value={inst.id}>{inst.name}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Equipment Cards Grid */}
      {loading ? (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-teal-600"></div>
        </div>
      ) : error ? (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-center">
          {error}
        </div>
      ) : equipmentList.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-slate-300">
          <Beaker className="w-12 h-12 text-slate-400 mx-auto mb-3" />
          <h3 className="text-lg font-semibold text-slate-800">No laboratory equipment in catalog</h3>
          <p className="text-sm text-slate-500 mt-1 max-w-md mx-auto">
            {canAddEquipment
              ? 'Click "Register Equipment" above to add your first piece of laboratory equipment.'
              : 'Log in as a Lab Manager or Administrator to register laboratory equipment.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {equipmentList.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between overflow-hidden"
            >
              <div className="p-5">
                <div className="flex items-start justify-between gap-2 mb-3">
                  <span className="text-xs font-semibold uppercase tracking-wider text-teal-700 bg-teal-50 px-2.5 py-1 rounded-md">
                    {item.category}
                  </span>
                  <EquipmentStatusBadge status={item.status} />
                </div>

                <h3 className="text-lg font-bold text-slate-900 tracking-tight leading-snug line-clamp-1">
                  {item.name}
                </h3>
                <p className="text-xs text-slate-500 font-mono mt-0.5">
                  Model: {item.modelNumber || 'N/A'} • SN: {item.serialNumber || 'N/A'}
                </p>

                <p className="text-xs text-slate-600 line-clamp-2 mt-2 leading-relaxed">
                  {item.description || 'No description provided.'}
                </p>

                <div className="mt-4 pt-3 border-t border-slate-100 space-y-1.5 text-xs text-slate-600">
                  <div className="flex items-center">
                    <MapPin className="w-3.5 h-3.5 mr-1.5 text-slate-400 flex-shrink-0" />
                    <span className="truncate">{item.locationRoom || 'Main Lab'} • {item.department?.name || 'Department'}</span>
                  </div>
                  <div className="flex items-center">
                    <Layers className="w-3.5 h-3.5 mr-1.5 text-slate-400 flex-shrink-0" />
                    <span className="truncate">{item.institution?.name || 'Institution'}</span>
                  </div>
                  {item.sharableInterInstitution && (
                    <div className="flex items-center text-teal-700 font-medium">
                      <Share2 className="w-3.5 h-3.5 mr-1.5 flex-shrink-0" />
                      <span>Inter-Institution Sharing Eligible</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Card Footer */}
              <div className="px-5 py-3.5 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                <div>
                  <span className="text-xs text-slate-500 block">Rate</span>
                  <span className="text-base font-bold text-slate-900">${item.hourlyRate}</span>
                  <span className="text-xs text-slate-500"> / hr</span>
                </div>

                <Link
                  to={`/equipment/${item.id}`}
                  className="px-4 py-2 bg-teal-700 hover:bg-teal-800 text-white text-xs font-semibold rounded-lg shadow-sm transition"
                >
                  View & Book
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Equipment Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-xl border border-slate-200">
            <div className="flex justify-between items-center pb-4 border-b border-slate-200">
              <h3 className="text-lg font-bold text-slate-900">Register New Equipment</h3>
              <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="w-5 h-5" />
              </button>
            </div>

            {modalError && (
              <div className="my-3 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg flex items-center space-x-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{modalError}</span>
              </div>
            )}

            <form onSubmit={handleCreateEquipment} className="mt-4 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-slate-700">Equipment Name *</label>
                  <input
                    type="text"
                    required
                    value={newEquip.name}
                    onChange={(e) => setNewEquip({ ...newEquip, name: e.target.value })}
                    placeholder="e.g. UV-Vis Spectrophotometer"
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">Category *</label>
                  <input
                    type="text"
                    required
                    value={newEquip.category}
                    onChange={(e) => setNewEquip({ ...newEquip, category: e.target.value })}
                    placeholder="e.g. Spectroscopy, Microscopy"
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">Model Number</label>
                  <input
                    type="text"
                    value={newEquip.modelNumber}
                    onChange={(e) => setNewEquip({ ...newEquip, modelNumber: e.target.value })}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">Serial Number</label>
                  <input
                    type="text"
                    value={newEquip.serialNumber}
                    onChange={(e) => setNewEquip({ ...newEquip, serialNumber: e.target.value })}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>

                <div>
                  <div className="flex justify-between items-center">
                    <label className="block text-xs font-medium text-slate-700">Institution *</label>
                    <button
                      type="button"
                      onClick={() => setShowQuickInst(!showQuickInst)}
                      className="text-[11px] text-teal-700 hover:underline font-semibold"
                    >
                      + Add New
                    </button>
                  </div>
                  {showQuickInst ? (
                    <div className="mt-1 flex gap-1">
                      <input
                        type="text"
                        placeholder="Institution name"
                        value={quickInstName}
                        onChange={(e) => setQuickInstName(e.target.value)}
                        className="w-full p-1.5 border border-slate-300 rounded text-xs"
                      />
                      <button
                        type="button"
                        onClick={handleQuickAddInstitution}
                        className="px-2 py-1 bg-teal-700 text-white text-xs rounded"
                      >
                        Save
                      </button>
                    </div>
                  ) : (
                    <select
                      value={newEquip.institutionId}
                      onChange={(e) => setNewEquip({ ...newEquip, institutionId: e.target.value })}
                      className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                    >
                      <option value="">-- Select Institution --</option>
                      {institutions.map((i) => (
                        <option key={i.id} value={i.id}>{i.name}</option>
                      ))}
                    </select>
                  )}
                </div>

                <div>
                  <div className="flex justify-between items-center">
                    <label className="block text-xs font-medium text-slate-700">Department *</label>
                    {newEquip.institutionId && (
                      <button
                        type="button"
                        onClick={() => setShowQuickDept(!showQuickDept)}
                        className="text-[11px] text-teal-700 hover:underline font-semibold"
                      >
                        + Add New
                      </button>
                    )}
                  </div>
                  {showQuickDept ? (
                    <div className="mt-1 flex gap-1">
                      <input
                        type="text"
                        placeholder="Department name"
                        value={quickDeptName}
                        onChange={(e) => setQuickDeptName(e.target.value)}
                        className="w-full p-1.5 border border-slate-300 rounded text-xs"
                      />
                      <button
                        type="button"
                        onClick={handleQuickAddDepartment}
                        className="px-2 py-1 bg-teal-700 text-white text-xs rounded"
                      >
                        Save
                      </button>
                    </div>
                  ) : (
                    <select
                      value={newEquip.departmentId}
                      onChange={(e) => setNewEquip({ ...newEquip, departmentId: e.target.value })}
                      className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                    >
                      <option value="">-- Select Department --</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      ))}
                    </select>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">Location / Room</label>
                  <input
                    type="text"
                    value={newEquip.locationRoom}
                    onChange={(e) => setNewEquip({ ...newEquip, locationRoom: e.target.value })}
                    placeholder="e.g. Lab Room 101"
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-700">Hourly Rate ($)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={newEquip.hourlyRate}
                    onChange={(e) => setNewEquip({ ...newEquip, hourlyRate: e.target.value })}
                    className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700">Description</label>
                <textarea
                  rows={2}
                  value={newEquip.description}
                  onChange={(e) => setNewEquip({ ...newEquip, description: e.target.value })}
                  placeholder="Overview of capabilities and use cases..."
                  className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700">Key Technical Specifications</label>
                <textarea
                  rows={2}
                  value={newEquip.specifications}
                  onChange={(e) => setNewEquip({ ...newEquip, specifications: e.target.value })}
                  placeholder="Key technical specifications..."
                  className="mt-1 w-full p-2 border border-slate-300 rounded-lg text-sm"
                />
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <input
                  type="checkbox"
                  id="sharable"
                  checked={newEquip.sharableInterInstitution}
                  onChange={(e) => setNewEquip({ ...newEquip, sharableInterInstitution: e.target.checked })}
                  className="rounded text-teal-600 focus:ring-teal-500"
                />
                <label htmlFor="sharable" className="text-xs text-slate-700 font-medium">
                  Allow Inter-Institution Resource Sharing for this equipment
                </label>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 border border-slate-300 text-slate-700 rounded-lg text-sm hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={modalLoading}
                  className="px-4 py-2 bg-teal-700 text-white rounded-lg text-sm hover:bg-teal-800 disabled:opacity-50"
                >
                  {modalLoading ? 'Registering...' : 'Register Equipment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
