import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from './api/client';
import { useAuth } from './auth/AuthContext';

function LoadingState() {
  return (
    <div className="loading">
      Loading career domains…
    </div>
  );
}

function ErrorBox({ message }) {
  if (!message) return null;

  return (
    <p className="error-box">
      {message}
    </p>
  );
}

function normalizeSkill(value) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

function getSkillName(value) {
  if (typeof value === 'string') {
    return value;
  }

  return (
    value?.name ||
    value?.skillName ||
    value?.skill ||
    ''
  );
}

export default function DomainExplorer() {
  const { token } = useAuth();

  const [domains, setDomains] = useState([]);
  const [roles, setRoles] = useState([]);
  const [profile, setProfile] = useState(null);

  const [selectedDomainId, setSelectedDomainId] =
    useState('');

  const [selectedRoleId, setSelectedRoleId] =
    useState('');

  const [roleDetails, setRoleDetails] =
    useState(null);

  const [search, setSearch] = useState('');

  const [loading, setLoading] =
    useState(true);

  const [rolesLoading, setRolesLoading] =
    useState(false);

  const [roleLoading, setRoleLoading] =
    useState(false);

  const [error, setError] = useState('');

  /*
   * Initial load:
   *
   * 1. Career domains
   * 2. Existing user profile
   *
   * We intentionally use /users/profile for the
   * user's skills because that endpoint is already
   * used successfully by the existing Profile and
   * Dashboard pages.
   */
  useEffect(() => {
    if (!token) {
      return;
    }

    let active = true;

    const loadInitialData = async () => {
      setLoading(true);
      setError('');

      try {
        const [domainData, profileData] =
          await Promise.all([
            api.domains.all(token),
            api.profile.get(token)
          ]);

        if (!active) {
          return;
        }

        const safeDomains = Array.isArray(
          domainData
        )
          ? domainData
          : [];

        setDomains(safeDomains);
        setProfile(profileData || null);

        if (safeDomains.length > 0) {
          setSelectedDomainId(
            String(safeDomains[0].id)
          );
        }
      } catch (err) {
        if (active) {
          setError(
            err?.message ||
              'Unable to load the career domain explorer.'
          );
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    loadInitialData();

    return () => {
      active = false;
    };
  }, [token]);

  /*
   * Load roles for selected domain.
   */
  useEffect(() => {
    if (!token || !selectedDomainId) {
      setRoles([]);
      setSelectedRoleId('');
      return;
    }

    let active = true;

    const loadRoles = async () => {
      setRolesLoading(true);
      setError('');

      try {
        const data =
          await api.domains.roles(
            selectedDomainId,
            token
          );

        if (!active) {
          return;
        }

        const safeRoles = Array.isArray(data)
          ? data
          : [];

        setRoles(safeRoles);

        if (safeRoles.length > 0) {
          setSelectedRoleId(
            String(safeRoles[0].id)
          );
        } else {
          setSelectedRoleId('');
        }
      } catch (err) {
        if (active) {
          setRoles([]);
          setSelectedRoleId('');
          setError(
            err?.message ||
              'Unable to load roles for this domain.'
          );
        }
      } finally {
        if (active) {
          setRolesLoading(false);
        }
      }
    };

    loadRoles();

    return () => {
      active = false;
    };
  }, [selectedDomainId, token]);

  /*
   * Load complete selected role.
   */
  useEffect(() => {
    if (!token || !selectedRoleId) {
      setRoleDetails(null);
      return;
    }

    let active = true;

    const loadRole = async () => {
      setRoleLoading(true);
      setError('');

      try {
        const data =
          await api.jobRoles.get(
            selectedRoleId,
            token
          );

        if (active) {
          setRoleDetails(data || null);
        }
      } catch (err) {
        if (active) {
          setRoleDetails(null);
          setError(
            err?.message ||
              'Unable to load role details.'
          );
        }
      } finally {
        if (active) {
          setRoleLoading(false);
        }
      }
    };

    loadRole();

    return () => {
      active = false;
    };
  }, [selectedRoleId, token]);

  const selectedDomain = useMemo(
    () =>
      domains.find(
        (domain) =>
          String(domain.id) ===
          String(selectedDomainId)
      ),
    [domains, selectedDomainId]
  );

  const filteredRoles = useMemo(() => {
    const query =
      search.trim().toLowerCase();

    if (!query) {
      return roles;
    }

    return roles.filter((role) =>
      String(
        role.name ||
          role.title ||
          ''
      )
        .toLowerCase()
        .includes(query)
    );
  }, [roles, search]);

  /*
   * Profile skills.
   *
   * The existing profile endpoint may return
   * skills as strings. This also tolerates objects.
   */
  const userSkills = useMemo(() => {
    const raw =
      profile?.skills ||
      profile?.userSkills ||
      [];

    return raw
      .map(getSkillName)
      .filter(Boolean);
  }, [profile]);

  /*
   * Required skills can be returned as strings
   * or skill objects depending on backend response.
   */
  const requiredSkills = useMemo(() => {
    const raw =
      roleDetails?.skills ||
      roleDetails?.requiredSkills ||
      [];

    return raw
      .map(getSkillName)
      .filter(Boolean);
  }, [roleDetails]);

  const normalizedUserSkills = useMemo(
    () =>
      new Set(
        userSkills
          .map(normalizeSkill)
          .filter(Boolean)
      ),
    [userSkills]
  );

  const matchedSkills = useMemo(
    () =>
      requiredSkills.filter((skill) =>
        normalizedUserSkills.has(
          normalizeSkill(skill)
        )
      ),
    [
      requiredSkills,
      normalizedUserSkills
    ]
  );

  const missingSkills = useMemo(
    () =>
      requiredSkills.filter(
        (skill) =>
          !normalizedUserSkills.has(
            normalizeSkill(skill)
          )
      ),
    [
      requiredSkills,
      normalizedUserSkills
    ]
  );

  const readiness = useMemo(() => {
    if (!requiredSkills.length) {
      return 0;
    }

    return Math.round(
      (matchedSkills.length /
        requiredSkills.length) *
        100
    );
  }, [
    requiredSkills,
    matchedSkills
  ]);

  if (loading) {
    return <LoadingState />;
  }

  return (
    <>
      <header className="page-header">
        <div>
          <p className="eyebrow">
            CAREER KNOWLEDGE BASE
          </p>

          <h1>Domain Explorer</h1>

          <p className="muted">
            Explore career domains, discover job
            roles, inspect graph-backed skill
            requirements and compare them with
            your current profile.
          </p>
        </div>
      </header>

      <ErrorBox message={error} />

      <section className="metric-grid">
        <article className="metric">
          <small>Career domains</small>
          <strong>
            {domains.length}
          </strong>
        </article>

        <article className="metric">
          <small>Roles in domain</small>
          <strong>
            {roles.length}
          </strong>
        </article>

        <article className="metric">
          <small>Your skills</small>
          <strong>
            {userSkills.length}
          </strong>
        </article>

        <article className="metric">
          <small>Role readiness</small>
          <strong>
            {readiness}%
          </strong>
        </article>
      </section>

      <section className="content-grid">
        <article className="panel">
          <p className="eyebrow">
            STEP 1
          </p>

          <h2>
            Choose a career domain
          </h2>

          <div className="card-grid">
            {domains.map((domain) => {
              const active =
                String(domain.id) ===
                String(selectedDomainId);

              return (
                <button
                  key={domain.id}
                  type="button"
                  className={
                    active
                      ? 'job-card chosen'
                      : 'job-card'
                  }
                  onClick={() => {
                    setSelectedDomainId(
                      String(domain.id)
                    );
                    setSearch('');
                  }}
                >
                  <p className="eyebrow">
                    CAREER DOMAIN
                  </p>

                  <h2>
                    {domain.name}
                  </h2>

                  <p className="muted">
                    {domain.description ||
                      'Explore roles, skills and career relationships in this domain.'}
                  </p>

                  <strong>
                    {domain.roleCount ?? 0}{' '}
                    roles
                  </strong>
                </button>
              );
            })}
          </div>

          {!domains.length && (
            <p className="empty">
              No career domains are available.
            </p>
          )}
        </article>

        <article className="panel">
          <p className="eyebrow">
            SELECTED DOMAIN
          </p>

          <h2>
            {selectedDomain?.name ||
              'Choose a domain'}
          </h2>

          <p className="muted">
            {selectedDomain?.description ||
              'Select a domain to view the career roles available within it.'}
          </p>

          {selectedDomainId && (
            <>
              <label>
                Search role
                <input
                  value={search}
                  placeholder="e.g. Backend Developer"
                  onChange={(event) =>
                    setSearch(
                      event.target.value
                    )
                  }
                />
              </label>

              {rolesLoading ? (
                <LoadingState />
              ) : (
                <div className="skill-list">
                  {filteredRoles.map(
                    (role) => {
                      const active =
                        String(role.id) ===
                        String(
                          selectedRoleId
                        );

                      return (
                        <button
                          key={role.id}
                          type="button"
                          className={
                            active
                              ? 'skill-remover chosen'
                              : 'skill-remover'
                          }
                          onClick={() =>
                            setSelectedRoleId(
                              String(role.id)
                            )
                          }
                        >
                          {role.name ||
                            role.title}
                        </button>
                      );
                    }
                  )}
                </div>
              )}

              {!rolesLoading &&
                !filteredRoles.length && (
                  <p className="empty">
                    No roles match your search.
                  </p>
                )}
            </>
          )}
        </article>
      </section>

      {roleLoading && (
        <LoadingState />
      )}

      {roleDetails &&
        !roleLoading && (
          <>
            <section className="content-grid">
              <article className="panel feature">
                <p className="eyebrow">
                  SELECTED CAREER ROLE
                </p>

                <h2>
                  {roleDetails.name ||
                    roleDetails.title}
                </h2>

                <p className="prose">
                  {roleDetails.description ||
                    'This role is connected to the platform knowledge graph and its associated skill requirements.'}
                </p>

                <div className="button-row">
                  <Link
                    className="secondary-button"
                    to={`/learning-path/${roleDetails.id}`}
                  >
                    Build learning path
                  </Link>

                  <Link
                    className="secondary-button"
                    to={`/graph/${roleDetails.id}`}
                  >
                    Open knowledge graph
                  </Link>

                  <Link
                    className="secondary-button"
                    to={`/assessments/${roleDetails.id}`}
                  >
                    Assess skills
                  </Link>
                </div>
              </article>

              <article className="panel">
                <p className="eyebrow">
                  ROLE READINESS
                </p>

                <div className="metric-inline">
                  <strong>
                    {readiness}%
                  </strong>

                  <span>
                    profile-to-role skill
                    coverage
                  </span>
                </div>

                <h3>
                  Matching skills
                </h3>

                {matchedSkills.length ? (
                  <div className="chips">
                    {matchedSkills.map(
                      (skill) => (
                        <span key={skill}>
                          {skill}
                        </span>
                      )
                    )}
                  </div>
                ) : (
                  <p className="muted">
                    No current skills match
                    this role yet.
                  </p>
                )}

                <h3>
                  Missing skills
                </h3>

                {missingSkills.length ? (
                  <div className="chips">
                    {missingSkills.map(
                      (skill) => (
                        <span key={skill}>
                          {skill}
                        </span>
                      )
                    )}
                  </div>
                ) : (
                  <p className="muted">
                    Your current skills cover
                    all listed role skills.
                  </p>
                )}
              </article>
            </section>

            <section className="panel">
              <p className="eyebrow">
                KNOWLEDGE GRAPH REQUIREMENTS
              </p>

              <h2>
                Required skills for this role
              </h2>

              {requiredSkills.length ? (
                <div className="chips">
                  {requiredSkills.map(
                    (skill) => (
                      <span key={skill}>
                        {skill}
                      </span>
                    )
                  )}
                </div>
              ) : (
                <p className="empty">
                  No graph-backed skills are
                  available for this role.
                </p>
              )}

              <p className="muted">
                These requirements are read from
                the knowledge-graph role
                relationships.
              </p>
            </section>

            <section className="panel">
              <p className="eyebrow">
                YOUR PROFILE
              </p>

              <h2>
                Current skill base
              </h2>

              {userSkills.length ? (
                <div className="chips">
                  {userSkills.map(
                    (skill, index) => (
                      <span
                        key={`${skill}-${index}`}
                      >
                        {skill}
                      </span>
                    )
                  )}
                </div>
              ) : (
                <p className="empty">
                  Add skills to your profile to
                  calculate role readiness.
                </p>
              )}
            </section>
          </>
        )}
    </>
  );
}