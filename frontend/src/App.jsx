import { useEffect, useState } from 'react';

import {

  NavLink,

  Navigate,

  Route,

  Routes,

  useNavigate,

  useParams

} from 'react-router-dom';

import { api } from './api/client';

import { useAuth } from './auth/AuthContext';
import DomainExplorer from './DomainExplorer';

const labels = {

  A: 'optionA',

  B: 'optionB',

  C: 'optionC',

  D: 'optionD'

};

const readable = (values) =>

  values?.length ? values.join(' · ') : 'None yet';

function Loader() {

  return (

    <div className="loading">

      Loading your career intelligence…

    </div>

  );

}

function Notice({ message, type = 'success' }) {

  if (!message) return null;

  return (

    <p className={type === 'error' ? 'error-box' : 'notice'}>

      {message}

    </p>

  );

}

function Score({ value = 0 }) {

  const numeric = Number(value || 0);

  const tone =

    numeric >= 70

      ? 'high'

      : numeric >= 35

        ? 'medium'

        : 'low';

  return (

    <span className={`score ${tone}`}>

      {numeric.toFixed(0)}%

    </span>

  );

}

function AuthScreen() {

  const { login } = useAuth();

  const [mode, setMode] = useState('login');

  const [form, setForm] = useState({

    name: '',

    email: '',

    password: ''

  });

  const [error, setError] = useState('');

  const [loading, setLoading] = useState(false);

  const submit = async (event) => {

    event.preventDefault();

    setLoading(true);

    setError('');

    try {

      if (mode === 'register') {

        await api.auth.register(form);

      }

      await login({

        email: form.email,

        password: form.password

      });

    } catch (error) {

      setError(error.message);

    } finally {

      setLoading(false);

    }

  };

  return (

    <main className="auth">

      <section className="hero">

        <p className="eyebrow">

          CAREER INTELLIGENCE PLATFORM

        </p>

        <h1>

          Turn your skills into a clearer career path.

        </h1>

        <p>

          Analyze job requirements, explore career

          relationships, measure readiness, identify

          skill gaps and generate an explainable learning

          path.

        </p>

        <div className="hero-points">

          <span>Neo4j knowledge graph</span>

          <span>BFS / DFS / shortest path</span>

          <span>TF–IDF matching</span>

          <span>Skill assessments</span>

        </div>

      </section>

      <form

        className="auth-card"

        onSubmit={submit}

      >

        <p className="eyebrow">

          {mode === 'login'

            ? 'WELCOME BACK'

            : 'CREATE YOUR PROFILE'}

        </p>

        <h2>

          {mode === 'login'

            ? 'Sign in'

            : 'Start your career map'}

        </h2>

        {mode === 'register' && (

          <label>

            Full name

            <input

              required

              value={form.name}

              placeholder="Your name"

              onChange={(event) =>

                setForm({

                  ...form,

                  name: event.target.value

                })

              }

            />

          </label>

        )}

        <label>

          Email address

          <input

            required

            type="email"

            value={form.email}

            placeholder="you@example.com"

            onChange={(event) =>

              setForm({

                ...form,

                email: event.target.value

              })

            }

          />

        </label>

        <label>

          Password

          <input

            required

            minLength={8}

            type="password"

            value={form.password}

            placeholder="At least 8 characters"

            onChange={(event) =>

              setForm({

                ...form,

                password: event.target.value

              })

            }

          />

        </label>

        <Notice

          message={error}

          type="error"

        />

        <button

          type="submit"

          disabled={loading}

        >

          {loading

            ? 'Please wait…'

            : mode === 'login'

              ? 'Sign in'

              : 'Create account'}

        </button>

        <button

          className="text-button"

          type="button"

          onClick={() => {

            setMode(

              mode === 'login'

                ? 'register'

                : 'login'

            );

            setError('');

          }}

        >

          {mode === 'login'

            ? 'Need an account? Register'

            : 'Already registered? Sign in'}

        </button>

      </form>

    </main>

  );

}

function Layout({ children }) {

  const { user, logout } = useAuth();

  const nav = [

    ['/dashboard', 'Dashboard'],

    ['/profile', 'Profile'],

    ['/jobs', 'Jobs'],

    ['/domains', 'Domain Explorer'],

    ['/ingest', 'Ingest job'],

    ['/recommendations', 'Matches'],

    ['/learning-path', 'Learning Path'],

    ['/graph', 'Graph'],

    ['/assessments', 'Assessments'],

    ['/insights', 'Insights']

  ];

  return (

    <div className="shell">

      <aside>

        <div className="brand">

          <span>CI</span>

          <div>

            Career<span>IQ</span>

          </div>

        </div>

        <nav>

          {nav.map(([path, label]) => (

            <NavLink

              key={path}

              to={path}

            >

              {label}

            </NavLink>

          ))}

        </nav>

        <div className="user-panel">

          <strong>{user?.name || 'User'}</strong>

          <small>{user?.email || ''}</small>

          <button

            className="text-button"

            type="button"

            onClick={logout}

          >

            Sign out

          </button>

        </div>

      </aside>

      <main className="workspace">

        {children}

      </main>

    </div>

  );

}

function Page({

  title,

  eyebrow,

  actions,

  children

}) {

  return (

    <>

      <header className="page-header">

        <div>

          <p className="eyebrow">

            {eyebrow || 'CAREER INTELLIGENCE'}

          </p>

          <h1>{title}</h1>

        </div>

        {actions}

      </header>

      {children}

    </>

  );

}

function Metric({ label, value }) {

  return (

    <article className="metric">

      <small>{label}</small>

      <strong>{value}</strong>

    </article>

  );

}

function ChipList({ values = [] }) {

  if (!values?.length) {

    return (

      <p className="muted">

        No data available yet.

      </p>

    );

  }

  return (

    <div className="chips">

      {values.map((value, index) => (

        <span key={`${value}-${index}`}>

          {value}

        </span>

      ))}

    </div>

  );

}

function Empty({ text }) {

  return (

    <p className="empty">

      {text}

    </p>

  );

}

/* ============================================================

   DASHBOARD

   ============================================================ */

function Dashboard() {

  const { token } = useAuth();

  const [data, setData] = useState(null);

  const [error, setError] = useState('');

  useEffect(() => {

    if (!token) return;

    setError('');

    Promise.all([

      api.profile.get(token),

      api.recommendations.mine(token),

      api.assessmentResults.mine(token),

      api.insights.mine(token)

    ])

      .then(

        ([

          profile,

          recommendations,

          history,

          insights

        ]) => {

          setData({

            profile,

            recommendations: recommendations || [],

            history: history || [],

            insights: insights || {}

          });

        }

      )

      .catch((error) => {

        setError(error.message);

      });

  }, [token]);

  if (!data && !error) {

    return <Loader />;

  }

  const best = data?.recommendations?.[0];

  return (

    <>

      <Page

        title="Your career command center"

        eyebrow="DASHBOARD"

      />

      <Notice

        message={error}

        type="error"

      />

      <section className="metric-grid">

        <Metric

          label="Profile complete"

          value={`${data?.profile?.completionPercentage || 0}%`}

        />

        <Metric

          label="Skills mapped"

          value={data?.profile?.skills?.length || 0}

        />

        <Metric

          label="Jobs analyzed"

          value={data?.recommendations?.length || 0}

        />

        <Metric

          label="Assessment average"

          value={`${data?.insights?.assessmentAverage || 0}%`}

        />

      </section>

      <section className="content-grid">

        <article className="panel feature">

          <p className="eyebrow">

            BEST CURRENT MATCH

          </p>

          {best ? (

            <>

              <h2>{best.title}</h2>

              <Score

                value={best.matchPercentage}

              />

              <p>

                {best.explanation ||

                  'This role has the strongest current skill similarity.'}

              </p>

              <p className="muted">

                Missing: {readable(best.missingSkills)}

              </p>

            </>

          ) : (

            <Empty text="Add skills and ingest jobs to calculate your first match." />

          )}

        </article>

        <article className="panel">

          <h2>Recommended next skills</h2>

          <ChipList

            values={data?.insights?.recommendedSkills}

          />

          <p className="muted">

            Derived from your strongest relevant

            career matches and assessment evidence.

          </p>

        </article>

        <article className="panel">

          <h2>Career direction</h2>

          <p>

            {data?.insights?.careerGoal ||

              'Add a career goal in your profile.'}

          </p>

          <h3>Suitable roles</h3>

          <ChipList

            values={data?.insights?.suitableRoles}

          />

        </article>

        <article className="panel">

          <h2>Assessment focus</h2>

          <p className="metric-inline">

            {data?.history?.length || 0}

            <span> submitted responses</span>

          </p>

          <p className="muted">

            Weak areas:{' '}

            {readable(

              data?.insights?.assessmentWeaknesses

            )}

          </p>

        </article>

      </section>

    </>

  );

}

/* ============================================================

   PROFILE

   ============================================================ */

function Profile() {

  const {

    token,

    updateUser

  } = useAuth();

  const [profile, setProfile] = useState(null);

  const [skill, setSkill] = useState('');

  const [message, setMessage] = useState('');

  const [error, setError] = useState('');

  const load = async () => {

    if (!token) return;

    try {

      const value = await api.profile.get(token);

      setProfile(value);

    } catch (error) {

      setError(error.message);

    }

  };

  useEffect(() => {

    load();

  }, [token]);

  if (!profile) {

    return (

      <>

        {error ? (

          <Page

            title="Profile"

            eyebrow="PROFILE"

          >

            <Notice

              message={error}

              type="error"

            />

          </Page>

        ) : (

          <Loader />

        )}

      </>

    );

  }

  const save = async (event) => {

    event.preventDefault();

    setMessage('');

    setError('');

    try {

      const value = await api.profile.update(

        profile,

        token

      );

      setProfile(value);

      updateUser(value);

      setMessage('Profile saved successfully.');

    } catch (error) {

      setError(error.message);

    }

  };

  const add = async (event) => {

    event.preventDefault();

    const trimmedSkill = skill.trim();

    if (!trimmedSkill) return;

    setError('');

    setMessage('');

    try {

      await api.skills.create(

        { skillName: trimmedSkill },

        token

      );

      setSkill('');

      await load();

      setMessage('Skill added successfully.');

    } catch (error) {

      setError(error.message);

    }

  };

  const remove = async (value) => {

    setError('');

    setMessage('');

    try {

      await api.skills.remove(

        value,

        token

      );

      await load();

      setMessage(`${value} removed.`);

    } catch (error) {

      setError(error.message);

    }

  };

  return (

    <>

      <Page

        title="Build your professional profile"

        eyebrow="PROFILE"

      />

      <Notice

        message={message}

      />

      <Notice

        message={error}

        type="error"

      />

      <section className="content-grid">

        <form

          className="panel"

          onSubmit={save}

        >

          <h2>Career context</h2>

          <label>

            Education

            <textarea

              value={profile.education || ''}

              placeholder="Degree, institution, certifications…"

              onChange={(event) =>

                setProfile({

                  ...profile,

                  education:

                    event.target.value

                })

              }

            />

          </label>

          <label>

            Experience

            <input

              value={profile.experience || ''}

              placeholder="e.g. Java backend development"

              onChange={(event) =>

                setProfile({

                  ...profile,

                  experience:

                    event.target.value

                })

              }

            />

          </label>

          <label>

            Career goal

            <textarea

              value={profile.careerGoal || ''}

              placeholder="e.g. Become a cloud-native Java developer"

              onChange={(event) =>

                setProfile({

                  ...profile,

                  careerGoal:

                    event.target.value

                })

              }

            />

          </label>

          <button type="submit">

            Save profile

          </button>

        </form>

        <article className="panel">

          <h2>Your skills</h2>

          <ChipList

            values={profile.skills}

          />

          <div className="skill-list">

            {(profile.skills || []).map(

              (value) => (

                <button

                  className="skill-remover"

                  key={value}

                  type="button"

                  onClick={() =>

                    remove(value)

                  }

                >

                  {value} ×

                </button>

              )

            )}

          </div>

          <form

            className="inline-form"

            onSubmit={add}

          >

            <input

              value={skill}

              required

              placeholder="e.g. Spring Boot, AWS"

              onChange={(event) =>

                setSkill(event.target.value)

              }

            />

            <button type="submit">

              Add skill

            </button>

          </form>

        </article>

      </section>

    </>

  );

}

/* ============================================================

   JOBS

   ============================================================ */

function Jobs() {

  const { token } = useAuth();

  const navigate = useNavigate();

  const [jobs, setJobs] = useState([]);

  const [search, setSearch] = useState('');

  const [error, setError] = useState('');

  const load = async (query = '') => {

    try {

      setError('');

      const values = await api.jobs.list(

        token,

        query

      );

      setJobs(values || []);

    } catch (error) {

      setError(error.message);

    }

  };

  useEffect(() => {

    load();

  }, [token]);

  return (

    <>

      <Page

        title="Job explorer"

        eyebrow="JOB KNOWLEDGE BASE"

        actions={

          <button

            type="button"

            onClick={() =>

              navigate('/ingest')

            }

          >

            Ingest a job

          </button>

        }

      />

      <Notice

        message={error}

        type="error"

      />

      <form

        className="search"

        onSubmit={(event) => {

          event.preventDefault();

          load(search.trim());

        }}

      >

        <input

          value={search}

          placeholder="Search role title"

          onChange={(event) =>

            setSearch(event.target.value)

          }

        />

        <button type="submit">

          Search

        </button>

      </form>

      <section className="card-grid">

        {jobs.map((job) => (

          <article

            className="job-card"

            key={job.id}

          >

            <p className="eyebrow">

              ANALYZED ROLE

            </p>

            <h2>{job.title}</h2>

            <p className="clamp">

              {job.description}

            </p>

            <ChipList

              values={job.skills || []}

            />

            <button

              className="secondary-button"

              type="button"

              onClick={() =>

                navigate(

                  `/jobs/${job.id}`

                )

              }

            >

              View analysis

            </button>

          </article>

        ))}

      </section>

      {!jobs.length && (

        <Empty text="No jobs found." />

      )}

    </>

  );

}

/* ============================================================

   JOB DETAIL

   ============================================================ */

function JobDetail() {

  const { token } = useAuth();

  const { id } = useParams();

  const navigate = useNavigate();

  const [job, setJob] = useState(null);

  const [gap, setGap] = useState(null);

  const [error, setError] = useState('');

  useEffect(() => {

    if (!id || !token) return;

    setError('');

    Promise.all([

      api.jobs.get(id, token),

      api.skillGaps.forJob(id, token)

    ])

      .then(([jobValue, gapValue]) => {

        setJob(jobValue);

        setGap(gapValue);

      })

      .catch((error) =>

        setError(error.message)

      );

  }, [id, token]);

  if (!job && !error) {

    return <Loader />;

  }

  return (

    <>

      <Page

        title={job?.title || 'Job details'}

        eyebrow="JOB ANALYSIS"

      />

      <Notice

        message={error}

        type="error"

      />

      {job && (

        <section className="content-grid">

          <article className="panel">

            <h2>Role description</h2>

            <p className="prose">

              {job.description}

            </p>

            <h3>

              Extracted skills

            </h3>

            <div className="confidence-list">

              {(job.extractedSkills || []).map(

                (skill) => (

                  <div

                    key={

                      skill.id ||

                      skill.name ||

                      skill.skillName

                    }

                  >

                    <span>

                      {skill.name ||

                        skill.skillName}

                    </span>

                    <small>

                      {Math.round(

                        Number(skill.confidence || 0) * 100

                      )}%

                    </small>

                  </div>

                )

              )}

            </div>

          </article>

          <article className="panel">

            <h2>Your readiness</h2>

            <Score

              value={

                gap?.matchPercentage ??

                gap?.readinessPercentage ??

                0

              }

            />

            <h3>

              Matching skills

            </h3>

            <ChipList

              values={

                gap?.matchingSkills ||

                gap?.matchedSkills ||

                []

              }

            />

            <h3>

              Skill gaps

            </h3>

            <ChipList

              values={gap?.missingSkills || []}

            />

            <div className="button-row">

              <button

                type="button"

                onClick={() =>

                  navigate(

                    `/learning-path/${id}`

                  )

                }

              >

                Build learning path

              </button>

              <button

                className="secondary-button"

                type="button"

                onClick={() =>

                  navigate(

                    `/graph/${id}`

                  )

                }

              >

                Open graph

              </button>

              <button

                className="secondary-button"

                type="button"

                onClick={() =>

                  navigate(

                    `/assessments/${id}`

                  )

                }

              >

                Take assessment

              </button>

            </div>

          </article>

        </section>

      )}

    </>

  );

}

/* ============================================================

   INGEST

   ============================================================ */

function Ingest() {

  const { token } = useAuth();

  const navigate = useNavigate();

  const [form, setForm] = useState({

    title: '',

    description: ''

  });

  const [error, setError] = useState('');

  const [loading, setLoading] = useState(false);

  const submit = async (event) => {

    event.preventDefault();

    setLoading(true);

    setError('');

    try {

      const job = await api.jobs.create(

        {

          title: form.title.trim(),

          description: form.description.trim()

        },

        token

      );

      navigate(`/jobs/${job.id}`);

    } catch (error) {

      setError(error.message);

    } finally {

      setLoading(false);

    }

  };

  return (

    <>

      <Page

        title="Ingest a job description"

        eyebrow="JOB ANALYSIS PIPELINE"

      />

      <form

        className="panel form-wide"

        onSubmit={submit}

      >

        <p>

          The platform extracts skills,

          stores the job in MySQL,

          creates graph relationships in Neo4j

          and generates skill-specific assessments.

        </p>

        <label>

          Role title

          <input

            required

            value={form.title}

            placeholder="e.g. Backend Java Engineer"

            onChange={(event) =>

              setForm({

                ...form,

                title: event.target.value

              })

            }

          />

        </label>

        <label>

          Job description

          <textarea

            required

            value={form.description}

            placeholder="Paste the complete job description, responsibilities and requirements…"

            onChange={(event) =>

              setForm({

                ...form,

                description:

                  event.target.value

              })

            }

          />

        </label>

        <Notice

          message={error}

          type="error"

        />

        <button

          type="submit"

          disabled={loading}

        >

          {loading

            ? 'Analyzing…'

            : 'Analyze and ingest job'}

        </button>

      </form>

    </>

  );

}

/* ============================================================

   RECOMMENDATIONS

   ============================================================ */

function Recommendations() {

  const { token } = useAuth();

  const navigate = useNavigate();

  const [recommendations, setRecommendations] =

    useState(null);

  const [error, setError] =

    useState('');

  useEffect(() => {

    if (!token) return;

    api.recommendations

      .mine(token)

      .then((values) =>

        setRecommendations(values || [])

      )

      .catch((error) =>

        setError(error.message)

      );

  }, [token]);

  if (!recommendations && !error) {

    return <Loader />;

  }

  return (

    <>

      <Page

        title="Your role matches"

        eyebrow="TF–IDF RECOMMENDATIONS"

      />

      <Notice

        message={error}

        type="error"

      />

      <section className="recommendation-list">

        {recommendations?.map(

          (recommendation) => (

            <article

              className="panel recommendation"

              key={recommendation.jobId}

            >

              <div>

                <p className="eyebrow">

                  COSINE SIMILARITY

                </p>

                <h2>

                  {recommendation.title}

                </h2>

                <p>

                  {recommendation.explanation ||

                    'This recommendation is based on skill similarity.'}

                </p>

                <h3>

                  Matching skills

                </h3>

                <ChipList

                  values={

                    recommendation.matchingSkills ||

                    []

                  }

                />

                <h3>

                  Missing skills

                </h3>

                <ChipList

                  values={

                    recommendation.missingSkills ||

                    []

                  }

                />

              </div>

              <div className="right-score">

                <Score

                  value={

                    recommendation.matchPercentage

                  }

                />

                <small>

                  Gap:{' '}

                  {Number(

                    recommendation.gapPercentage || 0

                  ).toFixed(2)}

                  %

                </small>

                <button

                  type="button"

                  onClick={() =>

                    navigate(

                      `/jobs/${recommendation.jobId}`

                    )

                  }

                >

                  Review gap

                </button>

              </div>

            </article>

          )

        )}

      </section>

      {!recommendations?.length && (

        <Empty text="Your matches will appear after you add skills and jobs." />

      )}

    </>

  );

}

/* ============================================================

   LEARNING PATH

   ============================================================ */

function LearningPath() {

  const { token } = useAuth();

  const { id } = useParams();

  const [jobs, setJobs] = useState([]);

  const [selectedJob, setSelectedJob] =

    useState(id || '');

  const [path, setPath] = useState(null);

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState('');

  useEffect(() => {

    if (!token) return;

    setError('');

    api.jobs

      .list(token)

      .then((values) => {

        const safeValues = values || [];

        setJobs(safeValues);

        if (id) {

          setSelectedJob(String(id));

        } else if (safeValues.length) {

          setSelectedJob(

            String(safeValues[0].id)

          );

        }

      })

      .catch((error) =>

        setError(error.message)

      );

  }, [token, id]);

  useEffect(() => {

    if (!selectedJob || !token) return;

    setPath(null);

    setError('');

    setLoading(true);

    api.learningPaths

      .forJob(selectedJob, token)

      .then((value) => {

        setPath(value);

      })

      .catch((error) => {

        setError(error.message);

      })

      .finally(() => {

        setLoading(false);

      });

  }, [selectedJob, token]);

  return (

    <>

      <Page

        title="Personalized learning path"

        eyebrow="PREREQUISITE-AWARE LEARNING"

      />

      <Notice

        message={error}

        type="error"

      />

      <div className="toolbar">

        <label className="compact-field">

          Target job

          <select

            value={selectedJob}

            onChange={(event) =>

              setSelectedJob(

                event.target.value

              )

            }

          >

            <option value="">

              Select a job

            </option>

            {jobs.map((job) => (

              <option

                key={job.id}

                value={job.id}

              >

                {job.title}

              </option>

            ))}

          </select>

        </label>

      </div>

      {loading && <Loader />}

      {!loading && !path && !error && jobs.length > 0 && (

        <Empty text="Select a target job to generate the learning path." />

      )}

      {!loading && !jobs.length && !error && (

        <Empty text="Ingest at least one job before generating a learning path." />

      )}

      {path && (

        <>

          <section className="metric-grid">

            <Metric

              label="Current readiness"

              value={`${Number(

                path.currentReadiness || 0

              ).toFixed(1)}%`}

            />

            <Metric

              label="Current skills"

              value={

                path.currentSkills?.length || 0

              }

            />

            <Metric

              label="Required skills"

              value={

                path.requiredSkills?.length || 0

              }

            />

            <Metric

              label="Learning steps"

              value={

                path.steps?.length || 0

              }

            />

          </section>

          <section className="content-grid">

            <article className="panel">

              <h2>Current skill base</h2>

              <ChipList

                values={

                  path.currentSkills || []

                }

              />

              <h3>Required for target role</h3>

              <ChipList

                values={

                  path.requiredSkills || []

                }

              />

              <h3>Remaining gaps</h3>

              <ChipList

                values={

                  path.missingSkills || []

                }

              />

            </article>

            <article className="panel">

              <p className="eyebrow">

                EXPLAINABLE RECOMMENDATION

              </p>

              <h2>

                Why this order?

              </h2>

              <p className="prose">

                {path.explanation ||

                  'The learning sequence is generated using your current skills, graph relationships, and prerequisite dependencies.'}

              </p>

            </article>

          </section>

          <section className="panel">

            <p className="muted">

              The sequence below respects explicit

              prerequisite relationships from the

              Neo4j knowledge graph.

            </p>

            {path.steps?.length ? (

              <div className="learning-timeline">

                {path.steps.map((step) => (

                  <article

                    className="learning-step"

                    key={`${step.skill}-${step.step}`}

                  >

                    <div className="step-number">

                      {step.step}

                    </div>

                    <div className="step-content">

                      <div className="step-header">

                        <div>

                          <p className="eyebrow">

                            LEARNING STEP

                          </p>

                          <h2>

                            {step.skill}

                          </h2>

                        </div>

                        <Score

                          value={step.priority}

                        />

                      </div>

                      <div className="step-meta">

                        <span>

                          Graph distance:{' '}

                          {Number(

                            step.graphDistance

                          ) < 0

                            ? 'Not connected'

                            : step.graphDistance}

                        </span>

                        <span>

                          Priority:{' '}

                          {Number(

                            step.priority || 0

                          ).toFixed(2)}

                        </span>

                      </div>

                      {step.graphPath?.length > 0 && (

                        <div className="path-box">

                          <strong>

                            Graph path

                          </strong>

                          <div>

                            {step.graphPath.join(

                              ' → '

                            )}

                          </div>

                        </div>

                      )}

                      <p>

                        {step.explanation ||

                          'Recommended because it contributes toward the target role.'}

                      </p>

                      {step.resources?.length > 0 && (

                        <div className="resource-list">

                          {step.resources.map(

                            (resource) => (

                              <a

                                className="resource-card"

                                key={

                                  resource.id ||

                                  `${step.skill}-${resource.title}`

                                }

                                href={resource.url}

                                target="_blank"

                                rel="noreferrer"

                              >

                                <strong>

                                  {resource.title}

                                </strong>

                                <span>

                                  {resource.type ||

                                    'Resource'}

                                  {' · '}

                                  {resource.difficulty ||

                                    'Standard'}

                                </span>

                                <small>

                                  ~

                                  {resource.estimatedHours ||

                                    0}{' '}

                                  hours

                                </small>

                              </a>

                            )

                          )}

                        </div>

                      )}

                    </div>

                  </article>

                ))}

              </div>

            ) : (

              <Empty text="No learning steps are required for the selected job." />

            )}

          </section>

        </>

      )}

    </>

  );

}

/* ============================================================

   KNOWLEDGE GRAPH

   ============================================================ */

function TraversalDiagram({ result, mode }) {
  const [zoom, setZoom] = useState(1);
  const [selected, setSelected] = useState('');
  const rawNodes = Array.isArray(result?.nodes) ? result.nodes : [];
  const nodes = rawNodes.map((node, index) => typeof node === 'string'
    ? { name: node, level: index, order: index }
    : node).filter((node) => node?.name);
  const edges = Array.isArray(result?.edges) ? result.edges : [];
  const levels = nodes.reduce((groups, node) => {
    const level = Number(node.level || 0);
    groups[level] = [...(groups[level] || []), node];
    return groups;
  }, {});

  if (!nodes.length) return <Empty text="No connected skills were found for this query. Try a skill from the knowledge base." />;

  return (
    <div className="traversal-visual" tabIndex="0" aria-label={`${mode} traversal graph`}>
      <div className="graph-toolbar">
        <div className="graph-legend">
        <span><i className="legend-dot start" /> Starting skill</span>
        <span><i className="legend-dot visited" /> Visited skill</span>
        <span><i className="legend-line" /> Connection used</span>
        </div>
        <div className="graph-controls" aria-label="Graph view controls">
          <button type="button" onClick={() => setZoom((value) => Math.min(1.5, value + 0.1))}>Zoom in</button>
          <button type="button" onClick={() => setZoom((value) => Math.max(0.7, value - 0.1))}>Zoom out</button>
          <button type="button" onClick={() => { setZoom(1); setSelected(''); }}>Reset view</button>
        </div>
      </div>
      <p className="graph-help">Scroll horizontally to pan when the graph is wider than the screen. Select a skill to highlight it.</p>
      <div className="traversal-pan">
      <div className={`traversal-levels ${mode === 'dfs' ? 'deep-path' : ''}`} style={{ transform: `scale(${zoom})`, transformOrigin: 'top left' }}>
        {Object.entries(levels).map(([level, levelNodes]) => (
          <section className="traversal-level" key={level}>
            <p>{mode === 'bfs' ? `Level ${level}` : `Depth ${level}`}</p>
            <div>{levelNodes.map((node) => (
              <button type="button" onClick={() => setSelected(node.name)} className={`traversal-node ${node.level === 0 ? 'start' : ''} ${selected === node.name ? 'selected' : ''}`} key={`${node.name}-${node.order}`}>
                {node.name}{mode === 'dfs' && <small>#{Number(node.order) + 1}</small>}
              </button>
            ))}</div>
          </section>
        ))}
      </div>
      </div>
      {edges.length > 0 && <details className="edge-details"><summary>Show the {edges.length} connections used</summary><div>{edges.map((edge, index) => <span key={`${edge.from}-${edge.to}-${index}`}>{edge.from} → {edge.to}</span>)}</div></details>}
    </div>
  );
}

function Graph() {

  const { token } = useAuth();

  const { id } = useParams();

  const [jobs, setJobs] = useState([]);

  const [selectedJob, setSelectedJob] =

    useState(id || '');

  const [graphData, setGraphData] =

    useState(null);

  const [skill, setSkill] =

    useState('Java');

  const [targetSkill, setTargetSkill] =

    useState('Spring Boot');

  const [depth, setDepth] =

    useState(3);

  const [algorithmResult, setAlgorithmResult] =

    useState(null);

  const [algorithm, setAlgorithm] =

    useState('');

  const [loading, setLoading] =

    useState(false);

  const [error, setError] =

    useState('');

  useEffect(() => {

    if (!token) return;

    api.jobs

      .list(token)

      .then((values) => {

        const safeValues = values || [];

        setJobs(safeValues);

        if (id) {

          setSelectedJob(String(id));

        } else if (!selectedJob && safeValues[0]) {

          setSelectedJob(

            String(safeValues[0].id)

          );

        }

      })

      .catch((error) =>

        setError(error.message)

      );

  }, [token, id]);

  useEffect(() => {

    if (!selectedJob || !token) return;

    api.jobs

      .graph(selectedJob, token)

      .then((value) => {

        setGraphData(value);

      })

      .catch((error) =>

        setError(error.message)

      );

  }, [selectedJob, token]);

  const runTraversal = async (type) => {

    if (!skill.trim()) {

      setError('Enter a starting skill.');

      return;

    }

    setError('');

    setLoading(true);

    setAlgorithm(type);

    try {

      const result =

        type === 'BFS'

          ? await api.graph.bfs(

              skill.trim(),

              depth,

              token

            )

          : await api.graph.dfs(

              skill.trim(),

              depth,

              token

            );

      setAlgorithmResult(result);

    } catch (error) {

      setError(error.message);

    } finally {

      setLoading(false);

    }

  };

  const runShortestPath = async () => {

    if (!skill.trim() || !targetSkill.trim()) {

      setError(

        'Enter both starting and target skills.'

      );

      return;

    }

    setError('');

    setLoading(true);

    setAlgorithm('SHORTEST PATH');

    try {

      const result =

        await api.graph.shortestPath(

          skill.trim(),

          targetSkill.trim(),

          token

        );

      setAlgorithmResult(result);

    } catch (error) {

      setError(error.message);

    } finally {

      setLoading(false);

    }

  };

  return (

    <>

      <Page

        title="Career Knowledge Graph"

        eyebrow="SKILL RELATIONSHIPS + GRAPH ALGORITHMS"

      />

      <Notice

        message={error}

        type="error"

      />

      <section className="content-grid">

        <article className="panel">

          <h2>See how a job connects to skills</h2>

          <label>

            Select analyzed job

            <select

              value={selectedJob}

              onChange={(event) =>

                setSelectedJob(

                  event.target.value

                )

              }

            >

              <option value="">

                Select a job

              </option>

              {jobs.map((job) => (

                <option

                  key={job.id}

                  value={job.id}

                >

                  {job.title}

                </option>

              ))}

            </select>

          </label>

          {graphData && (

            <>

              <div className="graph-nodes">

                {(graphData.nodes || []).map(

                  (node, index) => (

                    <div

                      key={

                        node.id ??

                        `${node.label}-${index}`

                      }

                      className={`node ${String(

                        node.type || ''

                      ).toLowerCase()}`}

                    >

                      <small>

                        {node.type || 'NODE'}

                      </small>

                      {node.label}

                    </div>

                  )

                )}

              </div>

              <div className="edge-list">

                {(graphData.relationships || []).map(

                  (edge, index) => {

                    const fromNode =

                      graphData.nodes?.find(

                        (node) =>

                          node.id === edge.from

                      );

                    const toNode =

                      graphData.nodes?.find(

                        (node) =>

                          node.id === edge.to

                      );

                    return (

                      <span

                        key={`${edge.from}-${edge.to}-${index}`}

                      >

                        {edge.type}:{' '}

                        {fromNode?.label ||

                          edge.from}{' '}

                        →

                        {' '}

                        {toNode?.label ||

                          edge.to}

                      </span>

                    );

                  }

                )}

              </div>

            </>

          )}

          {!graphData && !loading && (

            <Empty text="Select an analyzed job to inspect its graph." />

          )}

        </article>

        <article className="panel">

          <h2>Explore skill connections</h2>

          <label>

            Starting skill

            <input

              value={skill}

              onChange={(event) =>

                setSkill(

                  event.target.value

                )

              }

              placeholder="e.g. Java"

            />

          </label>

          <label>

            How far to explore

            <input

              type="number"

              min="1"

              max="10"

              value={depth}

              onChange={(event) =>

                setDepth(

                  Math.max(

                    1,

                    Math.min(

                      10,

                      Number(event.target.value) || 1

                    )

                  )

                )

              }

            />

          </label>

          <div className="button-row">

            <button

              type="button"

              onClick={() =>

                runTraversal('BFS')

              }

              disabled={loading}

            >

              Explore skills level by level

            </button>

            <button

              className="secondary-button"

              type="button"

              onClick={() =>

                runTraversal('DFS')

              }

              disabled={loading}

            >

              Explore one path deeply

            </button>

          </div>

          <h3>

            Find the shortest learning route

          </h3>

          <label>

            Target skill

            <input

              value={targetSkill}

              onChange={(event) =>

                setTargetSkill(

                  event.target.value

                )

              }

              placeholder="e.g. Spring Boot"

            />

          </label>

          <button

            type="button"

            onClick={runShortestPath}

            disabled={loading}

          >

            Find learning route

          </button>

          {loading && (

            <p className="muted">

              Exploring the knowledge graph…

            </p>

          )}

          {algorithmResult && (

            <div className="algorithm-result">

              {Array.isArray(

                algorithmResult.nodes

              ) ? (

                <>

                  <p className="eyebrow">

                    {algorithm === 'BFS' ? 'BREADTH-FIRST SEARCH (BFS)' : 'DEPTH-FIRST SEARCH (DFS)'}

                  </p>

                  <p>

                    {algorithm === 'BFS' ? 'Explore Skills Level by Level — ' : 'Explore One Skill Path Deeply — '}Starting skill:{' '}

                    <strong>

                      {algorithmResult.startSkill ||

                        skill}

                    </strong>

                  </p>

                  <p>

                    Search limit:{' '}

                    <strong>

                      {algorithmResult.maxDepth ??

                        depth}

                    </strong>

                  </p>

                  <p className="muted">{algorithmResult.explanation} This search runs in O(V + E) time for the explored graph.</p>
                  <TraversalDiagram result={algorithmResult} mode={algorithm === 'BFS' ? 'bfs' : 'dfs'} />

                </>

              ) : (

                <>

                  <p className="eyebrow">

                    SHORTEST PATH IN THE KNOWLEDGE GRAPH

                  </p>

                  <p>

                    Starting skill: {algorithmResult.startSkill || skill} → target skill: {algorithmResult.targetSkill || targetSkill}

                    <strong>

                      {algorithmResult.found ? 'Route found' : 'No route found'}

                    </strong>

                  </p>

                  <p>

                    {algorithmResult.found ? 'Skill connections: ' : ''}

                    <strong>

                      {algorithmResult.found ? `${algorithmResult.distance} · shortest-path search is O(V + E)` : 'Choose another pair of connected skills.'}

                    </strong>

                  </p>

                  {algorithmResult.path?.length ? (

                    <>
                    <div className="path-box">

                      {algorithmResult.path.join(

                        ' → '

                      )}

                    </div>

                    <p className="muted">{algorithmResult.explanation}</p>

                    </>

                  ) : (

                    <p className="muted">

                      No connected path was found.

                    </p>

                  )}

                </>

              )}

            </div>

          )}

        </article>

      </section>

    </>

  );

}

/* ============================================================

   ASSESSMENTS

   ============================================================ */

function Assessments() {

  const { token } = useAuth();

  const { id } = useParams();

  const navigate = useNavigate();

  const [jobs, setJobs] = useState([]);

  const [error, setError] = useState('');

  useEffect(() => {

    if (!token) return;

    api.jobs

      .list(token)

      .then((values) =>

        setJobs(values || [])

      )

      .catch((error) =>

        setError(error.message)

      );

  }, [token]);

  return (

    <>

      <Page

        title="Skill assessments"

        eyebrow="MEASURE UNDERSTANDING"

      />

      <Notice

        message={error}

        type="error"

      />

      <section className="card-grid">

        {jobs.map((job) => (

          <article

            className="job-card"

            key={job.id}

          >

            <p className="eyebrow">

              {(job.skills || []).length} SKILLS

            </p>

            <h2>

              {job.title}

            </h2>

            <p className="muted">

              Skill-specific questions

              generated from the job

              requirements.

            </p>

            <button

              type="button"

              onClick={() =>

                navigate(

                  `/assessments/${job.id}`

                )

              }

            >

              {id === String(job.id)

                ? 'Continue assessment'

                : 'Start assessment'}

            </button>

          </article>

        ))}

      </section>

      {!jobs.length && !error && (

        <Empty text="No analyzed jobs are available for assessment." />

      )}

      {id && (

        <AssessmentRun

          jobId={id}

        />

      )}

    </>

  );

}

function AssessmentRun({ jobId }) {

  const { token } = useAuth();

  const [questions, setQuestions] =

    useState(null);

  const [index, setIndex] =

    useState(0);

  const [selected, setSelected] =

    useState('');

  const [results, setResults] =

    useState([]);

  const [error, setError] =

    useState('');

  const [submitting, setSubmitting] =

    useState(false);

  useEffect(() => {
    if (!jobId || !token) return;

    let cancelled = false;

    setQuestions(null);
    setIndex(0);
    setResults([]);
    setSelected('');
    setError('');

    api.assessments
      .forJob(jobId, token)
      .then(async (values) => {
        if (cancelled) return;

        const loadedQuestions = values || [];
        setQuestions(loadedQuestions);

        if (!loadedQuestions.length) return;

        // Restore the latest saved answer for each question in this job.
        // If history cannot be loaded, the quiz remains usable.
        try {
          const history = await api.assessmentResults.mine(token);
          if (cancelled) return;

          const questionIds = new Set(
            loadedQuestions.map((question) => String(question.id))
          );
          const latestByQuestion = new Map();

          (history || []).forEach((entry) => {
            const id = String(entry.assessmentId);

            // History is returned newest first; keep the first answer per question.
            if (questionIds.has(id) && !latestByQuestion.has(id)) {
              latestByQuestion.set(id, entry);
            }
          });

          const isComplete = loadedQuestions.every((question) =>
            latestByQuestion.has(String(question.id))
          );

          if (isComplete) {
            setResults(
              loadedQuestions.map((question) => {
                const saved = latestByQuestion.get(String(question.id));

                return {
                  assessmentId: saved.assessmentId,
                  skill: saved.skill || question.skill,
                  score: Number(saved.score || 0),
                  correct: saved.correct === true,
                  resultSummary: saved.resultSummary || '',
                };
              })
            );
          }
        } catch {
          // If history retrieval fails, allow a new assessment to proceed.
        }
      })
      .catch((loadError) => {
        if (!cancelled) setError(loadError.message);
      });

    return () => {
      cancelled = true;
    };
  }, [jobId, token]);

  if (!questions && !error) {

    return <Loader />;

  }

  if (!questions?.length) {

    return (

      <Notice

        message={

          error ||

          'This job has no recognized skills to assess.'

        }

        type="error"

      />

    );

  }

  const question = questions[index];

  const submit = async (event) => {

    event.preventDefault();

    if (!selected || submitting) return;

    setSubmitting(true);

    setError('');

    try {

      const result =

        await api.assessments.submit(

          question.id,

          selected,

          token

        );

      const nextResults = [

        ...results,

        result

      ];

      setResults(nextResults);

      setSelected('');

      if (

        index <

        questions.length - 1

      ) {

        setIndex(index + 1);

      }

    } catch (error) {

      setError(error.message);

    } finally {

      setSubmitting(false);

    }

  };

  if (

    results.length ===

    questions.length

  ) {

    const score = Math.round(

      results.reduce(

        (sum, result) =>

          sum + Number(result.score || 0),

        0

      ) / results.length

    );

    return (

      <article className="panel result">

        <p className="eyebrow">

          ASSESSMENT COMPLETE

        </p>

        <h2>

          Final score

        </h2>

        <Score value={score} />

        <p>

          {

            results.filter(

              (result) =>

                result.correct

            ).length

          } of {results.length} questions

          answered correctly.

        </p>

        <h3>

          Topics to revisit

        </h3>

        <ChipList

          values={

            results

              .filter(

                (result) =>

                  !result.correct

              )

              .map(

                (result) =>

                  result.skill

              )

          }

        />

        <button
          type="button"
          onClick={() => {
            setResults([]);
            setIndex(0);
            setSelected('');
            setError('');
          }}
        >
          Retake assessment
        </button>
      </article>

    );

  }

  return (

    <article className="panel assessment">

      <div className="progress">

        <span

          style={{

            width: `${

              ((index + 1) /

                questions.length) *

              100

            }%`

          }}

        />

      </div>

      <p className="eyebrow">

        QUESTION {index + 1} OF{' '}

        {questions.length}

        {' · '}

        {question.skill}

      </p>

      <h2>

        {question.question}

      </h2>

      <form

        onSubmit={submit}

      >

        <div className="options">

          {Object.entries(labels).map(

            ([letter, key]) => (

              <label

                key={letter}

                className={

                  selected === letter

                    ? 'chosen'

                    : ''

                }

              >

                <input

                  required

                  type="radio"

                  name="answer"

                  value={letter}

                  checked={

                    selected === letter

                  }

                  onChange={() =>

                    setSelected(letter)

                  }

                />

                <b>{letter}</b>

                {question[key]}

              </label>

            )

          )}

        </div>

        <Notice

          message={error}

          type="error"

        />

        <button

          type="submit"

          disabled={submitting}

        >

          {submitting

            ? 'Submitting…'

            : 'Submit answer'}

        </button>

      </form>

    </article>

  );

}

/* ============================================================

   INSIGHTS

   ============================================================ */

function Insights() {

  const { token } = useAuth();

  const [data, setData] =

    useState(null);

  const [error, setError] =

    useState('');

  useEffect(() => {

    if (!token) return;

    api.insights

      .mine(token)

      .then((value) =>

        setData(value || {})

      )

      .catch((error) =>

        setError(error.message)

      );

  }, [token]);

  if (!data && !error) {

    return <Loader />;

  }

  return (

    <>

      <Page

        title="Career insights"

        eyebrow="PERSONALIZED EVIDENCE"

      />

      <Notice

        message={error}

        type="error"

      />

      <section className="content-grid">

        <article className="panel">

          <h2>

            Target direction

          </h2>

          <p>

            {data?.careerGoal ||

              'Set a career goal to make your insights more specific.'}

          </p>

          <h3>

            Strongest skills

          </h3>

          <ChipList

            values={

              data?.strongestSkills

            }

          />

          <h3>

            Skills to learn next

          </h3>

          <ChipList

            values={

              data?.recommendedSkills

            }

          />

        </article>

        <article className="panel">

          <h2>

            Evidence from your data

          </h2>

          <p>

            Assessment average:{' '}

            <Score

              value={

                data?.assessmentAverage || 0

              }

            />

          </p>

          <h3>

            Assessment weaknesses

          </h3>

          <ChipList

            values={

              data?.assessmentWeaknesses

            }

          />

          <h3>

            Suitable roles

          </h3>

          <ChipList

            values={

              data?.suitableRoles

            }

          />

        </article>

      </section>

    </>

  );

}

/* ============================================================

   ROUTING

   ============================================================ */

function Protected() {

  const { isAuthenticated } =

    useAuth();

  if (!isAuthenticated) {

    return <AuthScreen />;

  }

  return (

    <Layout>

      <Routes>

        <Route

          path="/dashboard"

          element={<Dashboard />}

        />

        <Route

          path="/profile"

          element={<Profile />}

        />

        <Route

          path="/jobs"

          element={<Jobs />}

        />

        <Route

          path="/jobs/:id"

          element={<JobDetail />}

        />

        <Route

          path="/ingest"

          element={<Ingest />}

        />

        <Route

          path="/recommendations"

          element={<Recommendations />}

        />

        <Route

          path="/learning-path"

          element={<LearningPath />}

        />

        <Route

          path="/learning-path/:id"

          element={<LearningPath />}

        />

        <Route

          path="/graph"

          element={<Graph />}

        />

        <Route

          path="/graph/:id"

          element={<Graph />}

        />

        <Route

          path="/assessments"

          element={<Assessments />}

        />

        <Route

          path="/assessments/:id"

          element={<Assessments />}

        />

        <Route

          path="/insights"

          element={<Insights />}

        />

        <Route

          path="/domains"

          element={<DomainExplorer />}

        />

        <Route

          path="*"

          element={

            <Navigate

              to="/dashboard"

              replace

            />

          }

        />

      </Routes>

    </Layout>

  );

}

export default function App() {

  return <Protected />;

}
