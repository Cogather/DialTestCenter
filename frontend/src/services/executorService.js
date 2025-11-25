import axios from 'axios'

const API_URL = '/api/executors'

export const getExecutors = async ({ page = 0, size = 20, status, keyword }) => {
  const params = { page, size }
  if (status !== undefined && status !== null) {
    params.status = status
  }
  if (keyword) {
    params.keyword = keyword
  }
  return axios.get(API_URL, { params })
}

export const refreshExecutor = async (name) => {
  return axios.post(`${API_URL}/refresh`, { name })
}

// This might need to be added to the backend if we want detail by ID without refresh
// Assuming we can get detail from the list or need a new endpoint.
// The current backend seems to rely on WebSocket for updates or "refresh" command.
// However, we might want to fetch the current detailed state including UEs.
// If /api/executors/{name} exists, we use it. If not, we might need to rely on the list or add it.
// Checking the code, there isn't a detail endpoint in ExecutorController.
// We might need to add one or use the info from the list (which lacks UEs).
// BUT, the `refresh` endpoint triggers an update request to the agent.
// Maybe we need a `getExecutorDetail` endpoint.
// For now, I will assume we might need to add it. I'll create the service method anticipating it.
export const getExecutorDetail = async (name) => {
    // Validating if this endpoint exists... it does NOT in the controller I saw.
    // I will implement the backend endpoint for this.
  return axios.get(`${API_URL}/${name}`)
}

