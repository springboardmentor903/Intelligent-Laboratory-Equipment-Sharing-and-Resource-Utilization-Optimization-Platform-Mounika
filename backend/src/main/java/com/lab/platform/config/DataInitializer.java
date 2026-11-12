package com.lab.platform.config;

import com.lab.platform.auth.Role;
import com.lab.platform.auth.User;
import com.lab.platform.auth.UserRepository;
import com.lab.platform.equipment.Equipment;
import com.lab.platform.equipment.EquipmentRepository;
import com.lab.platform.equipment.EquipmentStatus;
import com.lab.platform.institution.Department;
import com.lab.platform.institution.DepartmentRepository;
import com.lab.platform.institution.Institution;
import com.lab.platform.institution.InstitutionRepository;
import com.lab.platform.sharing.SharingListing;
import com.lab.platform.sharing.SharingListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Clean system startup initializer.
 * Bootstraps the platform System Administrator exclusively from environment variables / configuration.
 * Seeds initial institutions, departments, sample equipment, and test accounts.
 * No public API or registration can create a System Administrator.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final DepartmentRepository departmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final SharingListingRepository sharingListingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.system-admin.email:admin@labplatform.local}")
    private String systemAdminEmail;

    @Value("${app.system-admin.password:Admin@123456}")
    private String systemAdminPassword;

    public DataInitializer(UserRepository userRepository,
                           InstitutionRepository institutionRepository,
                           DepartmentRepository departmentRepository,
                           EquipmentRepository equipmentRepository,
                           SharingListingRepository sharingListingRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.departmentRepository = departmentRepository;
        this.equipmentRepository = equipmentRepository;
        this.sharingListingRepository = sharingListingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed initial institutions and departments if empty
        if (institutionRepository.count() == 0) {
            Institution ait = institutionRepository.save(new Institution(
                    "Apex Institute of Technology", "AIT", "Tech Park Campus, Bangalore", "info@apex.edu", "+91-80-23456789"));
            Institution bnu = institutionRepository.save(new Institution(
                    "BioScience National University", "BNU", "Science City, Hyderabad", "registrar@bnu.ac.in", "+91-40-87654321"));

            departmentRepository.save(new Department("Computer Science & Engineering", "CSE", ait));
            departmentRepository.save(new Department("Biomedical Engineering", "BME", ait));
            departmentRepository.save(new Department("Molecular Biology & Genetics", "MBG", bnu));
            departmentRepository.save(new Department("Analytical Chemistry", "CHEM", bnu));

            logger.info("Bootstrap: Initial sample institutions and departments seeded successfully.");
        }

        // 2. Provision System Administrator
        if (userRepository.countByRole(Role.SYSTEM_ADMIN) == 0) {
            String email = systemAdminEmail.toLowerCase().trim();
            User admin = userRepository.findByEmail(email).orElse(new User());
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(systemAdminPassword));
            admin.setFullName("Platform System Administrator");
            admin.setRole(Role.SYSTEM_ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
            logger.info("Bootstrap: Initial System Administrator provisioned successfully with email: {}", email);
        } else {
            logger.info("System Administrator exists. Startup check complete.");
        }

        // 3. Provision Sample Role Accounts for Testing and Mentor Demonstration
        Department bmeDept = departmentRepository.findByCode("BME").orElse(null);
        Institution aitInst = institutionRepository.findByCode("AIT").orElse(null);

        if (bmeDept != null && aitInst != null) {
            // Researcher / Student Account
            if (userRepository.findByEmail("student@apex.edu").isEmpty()) {
                User student = new User();
                student.setEmail("student@apex.edu");
                student.setPasswordHash(passwordEncoder.encode("User@123456"));
                student.setFullName("Priya Sharma (Student Researcher)");
                student.setRole(Role.RESEARCHER_STUDENT);
                student.setInstitution(aitInst);
                student.setDepartment(bmeDept);
                student.setActive(true);
                userRepository.save(student);
                logger.info("Bootstrap: Sample Researcher provisioned: student@apex.edu / User@123456");
            }

            // Lab Manager Account
            if (userRepository.findByEmail("manager@apex.edu").isEmpty()) {
                User manager = new User();
                manager.setEmail("manager@apex.edu");
                manager.setPasswordHash(passwordEncoder.encode("User@123456"));
                manager.setFullName("Dr. Rajesh Kumar (Lab Manager)");
                manager.setRole(Role.LAB_MANAGER);
                manager.setInstitution(aitInst);
                manager.setDepartment(bmeDept);
                manager.setActive(true);
                userRepository.save(manager);
                logger.info("Bootstrap: Sample Lab Manager provisioned: manager@apex.edu / User@123456");
            }

            // Lab Technician Account
            if (userRepository.findByEmail("technician@apex.edu").isEmpty()) {
                User technician = new User();
                technician.setEmail("technician@apex.edu");
                technician.setPasswordHash(passwordEncoder.encode("User@123456"));
                technician.setFullName("Suresh Verma (Lab Technician)");
                technician.setRole(Role.LAB_TECHNICIAN);
                technician.setInstitution(aitInst);
                technician.setDepartment(bmeDept);
                technician.setActive(true);
                userRepository.save(technician);
                logger.info("Bootstrap: Sample Lab Technician provisioned: technician@apex.edu / User@123456");
            }

            // Department Head Account
            if (userRepository.findByEmail("hod.bme@apex.edu").isEmpty()) {
                User hod = new User();
                hod.setEmail("hod.bme@apex.edu");
                hod.setPasswordHash(passwordEncoder.encode("User@123456"));
                hod.setFullName("Prof. Anita Desai (Department Head)");
                hod.setRole(Role.DEPARTMENT_HEAD);
                hod.setInstitution(aitInst);
                hod.setDepartment(bmeDept);
                hod.setActive(true);
                userRepository.save(hod);
                logger.info("Bootstrap: Sample Department Head provisioned: hod.bme@apex.edu / User@123456");
            }

            // Institution Admin Account
            if (userRepository.findByEmail("admin@apex.edu").isEmpty()) {
                User instAdmin = new User();
                instAdmin.setEmail("admin@apex.edu");
                instAdmin.setPasswordHash(passwordEncoder.encode("User@123456"));
                instAdmin.setFullName("Dean Office (Apex Institution Admin)");
                instAdmin.setRole(Role.INSTITUTION_ADMIN);
                instAdmin.setInstitution(aitInst);
                instAdmin.setDepartment(bmeDept);
                instAdmin.setActive(true);
                userRepository.save(instAdmin);
                logger.info("Bootstrap: Sample Institution Admin provisioned: admin@apex.edu / User@123456");
            }

            // 4. Provision Initial Sample Equipment if empty
            if (equipmentRepository.count() == 0) {
                Equipment eq1 = new Equipment();
                eq1.setName("Confocal Laser Scanning Microscope (Leica SP8)");
                eq1.setAssetTag("EQ-MIC-001");
                eq1.setManufacturer("Leica Microsystems");
                eq1.setModelNumber("SP8-X");
                eq1.setSerialNumber("SN-88210");
                eq1.setCategory("Microscopy");
                eq1.setTags("microscopy,confocal,fluorescence,laser");
                eq1.setDescription("High-resolution spectral confocal microscope with pulsed white light laser and hybrid detectors for deep cellular imaging.");
                eq1.setLocationRoom("Room 402, Optics Wing");
                eq1.setStatus(EquipmentStatus.AVAILABLE);
                eq1.setHourlyRate(new BigDecimal("75.00"));
                eq1.setRequiresApproval(true);
                eq1.setRequiresTraining(false);
                eq1.setSharableInterInstitution(true);
                eq1.setInstitution(aitInst);
                eq1.setDepartment(bmeDept);
                equipmentRepository.save(eq1);

                Equipment eq2 = new Equipment();
                eq2.setName("High-Resolution Q-TOF Mass Spectrometer");
                eq2.setAssetTag("EQ-SPEC-002");
                eq2.setManufacturer("Waters Corporation");
                eq2.setModelNumber("Synapt G2-Si");
                eq2.setSerialNumber("SN-99411");
                eq2.setCategory("Spectroscopy");
                eq2.setTags("mass-spectrometry,q-tof,proteomics,analytical");
                eq2.setDescription("High-performance quadrupole time-of-flight mass spectrometer with ion mobility separation for molecular discovery.");
                eq2.setLocationRoom("Room 105, Analytical Core");
                eq2.setStatus(EquipmentStatus.AVAILABLE);
                eq2.setHourlyRate(new BigDecimal("120.00"));
                eq2.setRequiresApproval(false);
                eq2.setRequiresTraining(true);
                eq2.setSharableInterInstitution(true);
                eq2.setInstitution(aitInst);
                eq2.setDepartment(bmeDept);
                equipmentRepository.save(eq2);

                logger.info("Bootstrap: Initial sample equipment seeded (Microscope & Mass Spectrometer).");
            }
        }

        // 4. Provision BioScience National University (BNU) sample accounts and equipment for inter-institution testing
        Institution bnuInst = institutionRepository.findByCode("BNU").orElse(null);
        Department mbgDept = departmentRepository.findByCode("MBG").orElse(null);

        if (bnuInst != null && mbgDept != null) {
            if (userRepository.findByEmail("researcher@bnu.ac.in").isEmpty()) {
                User bnuStudent = new User();
                bnuStudent.setEmail("researcher@bnu.ac.in");
                bnuStudent.setPasswordHash(passwordEncoder.encode("User@123456"));
                bnuStudent.setFullName("Arun Patel (BNU Researcher)");
                bnuStudent.setRole(Role.RESEARCHER_STUDENT);
                bnuStudent.setInstitution(bnuInst);
                bnuStudent.setDepartment(mbgDept);
                bnuStudent.setActive(true);
                userRepository.save(bnuStudent);
                logger.info("Bootstrap: Sample BNU Researcher provisioned: researcher@bnu.ac.in / User@123456");
            }

            if (userRepository.findByEmail("admin@bnu.ac.in").isEmpty()) {
                User bnuAdmin = new User();
                bnuAdmin.setEmail("admin@bnu.ac.in");
                bnuAdmin.setPasswordHash(passwordEncoder.encode("User@123456"));
                bnuAdmin.setFullName("Dean Research (BNU Institution Admin)");
                bnuAdmin.setRole(Role.INSTITUTION_ADMIN);
                bnuAdmin.setInstitution(bnuInst);
                bnuAdmin.setDepartment(mbgDept);
                bnuAdmin.setActive(true);
                userRepository.save(bnuAdmin);
                logger.info("Bootstrap: Sample BNU Institution Admin provisioned: admin@bnu.ac.in / User@123456");
            }

            if (equipmentRepository.findByAssetTag("EQ-SEQ-003").isEmpty()) {
                Equipment eq3 = new Equipment();
                eq3.setName("High-Throughput DNA Sequencer (Illumina NovaSeq 6000)");
                eq3.setAssetTag("EQ-SEQ-003");
                eq3.setManufacturer("Illumina, Inc.");
                eq3.setModelNumber("NovaSeq 6000");
                eq3.setSerialNumber("SN-SEQ-7712");
                eq3.setCategory("Genomics");
                eq3.setTags("genomics,sequencing,dna,ngs");
                eq3.setDescription("Ultra-high-throughput sequencing system enabling comprehensive genomics applications across large cohorts.");
                eq3.setLocationRoom("Room 201, Genomics Pavilion");
                eq3.setStatus(EquipmentStatus.AVAILABLE);
                eq3.setHourlyRate(new BigDecimal("150.00"));
                eq3.setRequiresApproval(false);
                eq3.setRequiresTraining(false);
                eq3.setSharableInterInstitution(true);
                eq3.setInstitution(bnuInst);
                eq3.setDepartment(mbgDept);
                equipmentRepository.save(eq3);
                logger.info("Bootstrap: Sample BNU Equipment seeded: EQ-SEQ-003");
            }
        }

        // 5. Seed default Sharing Listings for Inter-Institution discovery (Session 4)
        if (sharingListingRepository.count() == 0) {
            equipmentRepository.findByAssetTag("EQ-SPEC-002").ifPresent(eq -> {
                SharingListing listing = new SharingListing(
                        eq,
                        new BigDecimal("130.00"),
                        25,
                        24,
                        4,
                        "Open to all accredited academic partner institutions. Prior safety orientation recommended.",
                        true
                );
                sharingListingRepository.save(listing);
                logger.info("Bootstrap: Active sharing listing created for EQ-SPEC-002 (Apex)");
            });

            equipmentRepository.findByAssetTag("EQ-SEQ-003").ifPresent(eq -> {
                SharingListing listing = new SharingListing(
                        eq,
                        new BigDecimal("160.00"),
                        30,
                        48,
                        6,
                        "Collaborative research access available under standard consortium MoU.",
                        true
                );
                sharingListingRepository.save(listing);
                logger.info("Bootstrap: Active sharing listing created for EQ-SEQ-003 (BNU)");
            });
        }
    }
}
