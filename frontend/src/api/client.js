const API_BASE = '/api';

async function request(path, options = {}) {
  const {
    token,
    method = 'GET',
    body
  } = options;

  const headers = {
    Accept: 'application/json'
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body:
      body !== undefined
        ? JSON.stringify(body)
        : undefined
  });

  const text = await response.text();

  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      typeof data === 'object' && data
        ? data.message ||
          data.error ||
          `Request failed with status ${response.status}`
        : String(
            data ||
              `Request failed with status ${response.status}`
          );

    throw new Error(message);
  }

  return data;
}

export const api = {
  auth: {
    register: (payload) =>
      request('/auth/register', {
        method: 'POST',
        body: payload
      }),

    login: (payload) =>
      request('/auth/login', {
        method: 'POST',
        body: payload
      })
  },

  profile: {
    get: (token) =>
      request('/users/me/profile', {
        token
      }),

    update: (payload, token) =>
      request('/users/me/profile', {
        method: 'PUT',
        token,
        body: payload
      })
  },

  skills: {
    getMine: (token) =>
      request('/users/me/skills', {
        token
      }),

    create: (payload, token) =>
      request('/users/me/skills', {
        method: 'POST',
        token,
        body: payload
      }),

    remove: (skillName, token) =>
      request(
        `/users/me/skills/${encodeURIComponent(skillName)}`,
        {
          method: 'DELETE',
          token
        }
      ),

    search: (query, token) =>
      request(
        `/skills?search=${encodeURIComponent(query || '')}`,
        {
          token
        }
      )
  },

  jobs: {
    list: (token, search = '') =>
      request(
        `/jobs${
          search
            ? `?search=${encodeURIComponent(search)}`
            : ''
        }`,
        {
          token
        }
      ),

    get: (id, token) =>
      request(`/jobs/${id}`, {
        token
      }),

    create: (payload, token) =>
      request('/jobs', {
        method: 'POST',
        token,
        body: payload
      }),

    graph: (id, token) =>
      request(`/jobs/${id}/graph`, {
        token
      })
  },

  recommendations: {
    mine: (token) =>
      request('/recommendations/me', {
        token
      })
  },

  skillGaps: {
    forJob: (id, token) =>
      request(`/skill-gaps/jobs/${id}`, {
        token
      })
  },

  assessmentResults: {
    mine: (token) =>
      request('/assessment-results/me', {
        token
      })
  },

  assessments: {
    forJob: (jobId, token) =>
      request(`/assessments/job/${jobId}`, {
        token
      }),

    submit: (assessmentId, selectedAnswer, token) =>
      request(
        `/assessments/${assessmentId}/submit`,
        {
          method: 'POST',
          token,
          body: {
            selectedAnswer
          }
        }
      )
  },

  insights: {
    mine: (token) =>
      request('/insights/me', {
        token
      })
  },

  learningPaths: {
    forJob: (jobId, token) =>
      request(`/learning-path/jobs/${jobId}`, {
        token
      })
  },

  graph: {
    bfs: (skill, depth, token) =>
      request(
        `/graph/bfs/${encodeURIComponent(skill)}?depth=${encodeURIComponent(depth)}`,
        {
          token
        }
      ),

    dfs: (skill, depth, token) =>
      request(
        `/graph/dfs/${encodeURIComponent(skill)}?depth=${encodeURIComponent(depth)}`,
        {
          token
        }
      ),

    shortestPath: (
      startSkill,
      targetSkill,
      token
    ) =>
      request(
        `/graph/shortest-path?from=${encodeURIComponent(
          startSkill
        )}&to=${encodeURIComponent(
          targetSkill
        )}`,
        {
          token
        }
      )
  },

  domains: {
    all: (token) =>
      request('/domains', {
        token
      }),

    get: (id, token) =>
      request(`/domains/${id}`, {
        token
      }),

    roles: (domainId, token) =>
      request(`/domains/${domainId}/roles`, {
        token
      })
  },

  jobRoles: {
    all: (token, search = '') =>
      request(
        `/job-roles${
          search
            ? `?search=${encodeURIComponent(search)}`
            : ''
        }`,
        {
          token
        }
      ),

    get: (id, token) =>
      request(`/job-roles/${id}`, {
        token
      }),

    skills: (id, token) =>
      request(`/job-roles/${id}/skills`, {
        token
      })
  }
};

export default api;
