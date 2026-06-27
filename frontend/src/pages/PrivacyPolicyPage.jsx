import { Link } from "react-router-dom";
import "./PrivacyPolicyPage.css";

export default function PrivacyPolicyPage() {
  return (
    <main className="privacy-page">
      <section className="privacy-card">
        <div className="privacy-header">
          <Link to="/" className="privacy-back-link">
            ← Back to DAYA Log
          </Link>

          <img
            src="/DAYA_Logo.svg"
            alt="DAYA Log"
            className="privacy-logo"
          />

          <p className="privacy-kicker">DAYA Log</p>
          <h1>Privacy Policy</h1>
          <p className="privacy-updated">Last updated: 28 June 2026</p>
        </div>

        <div className="privacy-content">
          <p>
            This Privacy Policy explains how DAYA Log collects, uses, stores,
            and protects information when you use the DAYA Log website and
            mobile applications.
          </p>

          <h2>1. About DAYA Log</h2>
          <p>
            DAYA Log is a Pilates studio management application designed to help
            manage appointments, clients, instructors, services, and daily studio
            activity.
          </p>

          <h2>2. Information We Collect</h2>
          <p>
            Depending on how the application is used, DAYA Log may collect and
            process the following types of information:
          </p>

          <ul>
            <li>Account information, such as name, email address, and role.</li>
            <li>Client profile information used for studio administration.</li>
            <li>Appointment details, including dates, times, services, and status.</li>
            <li>Instructor and service information.</li>
            <li>Technical information required to keep the application secure and functional.</li>
          </ul>

          <h2>3. How We Use Information</h2>
          <p>
            The information is used only for the operation of the DAYA Log
            platform, including:
          </p>

          <ul>
            <li>Managing user access and authentication.</li>
            <li>Managing Pilates studio appointments.</li>
            <li>Managing clients, instructors, and services.</li>
            <li>Improving application reliability, security, and performance.</li>
            <li>Providing administrative features to authorised users.</li>
          </ul>

          <h2>4. Login and Authentication</h2>
          <p>
            DAYA Log uses secure authentication to protect access to the
            application. Users must log in with authorised credentials to access
            protected areas of the platform.
          </p>

          <h2>5. Data Sharing</h2>
          <p>
            We do not sell personal data. Information may be processed by trusted
            service providers that help us operate the application, such as
            hosting, database, infrastructure, and email services.
          </p>

          <h2>6. Data Storage and Security</h2>
          <p>
            DAYA Log uses technical and organisational measures to protect data
            against unauthorised access, loss, misuse, or alteration. However, no
            online system can be guaranteed to be completely secure.
          </p>

          <h2>7. Data Retention</h2>
          <p>
            Information is kept only for as long as necessary to provide the
            application services, manage studio activity, comply with legal
            obligations, resolve disputes, and maintain security.
          </p>

          <h2>8. User Rights</h2>
          <p>
            Depending on your location and applicable law, you may have the right
            to request access to your data, correction of inaccurate data,
            deletion of data, restriction of processing, or information about how
            your data is used.
          </p>

          <h2>9. Children</h2>
          <p>
            DAYA Log is intended for studio management and administrative use. It
            is not directed to children. If information about a minor is handled
            by a studio, it must be managed by authorised staff and in accordance
            with applicable law.
          </p>

          <h2>10. Changes to This Policy</h2>
          <p>
            We may update this Privacy Policy from time to time. Any changes will
            be published on this page with an updated date.
          </p>

          <h2>11. Contact</h2>
          <p>
            If you have questions about this Privacy Policy or how DAYA Log
            handles data, you can contact us at:
          </p>

          <p>
            <a href="mailto:contact@hypso25.ro">contact@hypso25.ro</a>
          </p>

          <p className="privacy-note">
            This Privacy Policy is provided for DAYA Log and should be reviewed
            before public release to make sure it matches the final business,
            legal, and data-processing setup of the application.
          </p>
        </div>
      </section>
    </main>
  );
}