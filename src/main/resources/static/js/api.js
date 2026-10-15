const API_URL = "/api/reports";

async function request(url, options) {
    const response = await fetch(url, options);
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.message || "Something went wrong");
    }
    return data;
}

function jsonOptions(method, body) {
    return {
        method: method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    };
}

function showMessage(element, text, type = "default") {
    element.className = type === "default"
        ? "message"
        : "message message-" + type;

    element.textContent = text;
}

function getReports(filters) {
    const params = new URLSearchParams();
    for (const [name, value] of Object.entries(filters || {})) {
        if (value) {
            params.append(name, value);
        }
    }
    return request(API_URL + "?" + params);
}

function getReport(id) {
    return request(API_URL + "/" + id);
}

function createReport(report) {
    return request(API_URL, jsonOptions("POST", report));
}

function upvoteReport(id, voterEmail) {
    return request(API_URL + "/" + id + "/upvotes", jsonOptions("POST", { voterEmail: voterEmail }));
}

function updateStatus(id, status) {
    return request(API_URL + "/" + id + "/status", jsonOptions("PATCH", { status: status }));
}

function getStats() {
    return request(API_URL + "/stats");
}

function formatLabel(value) {
    const text = value.replace("_", " ").toLowerCase();
    return text.charAt(0).toUpperCase() + text.slice(1);
}

function badgeClass(status) {
    return "badge badge-" + status.toLowerCase().replace("_", "-");
}

function formatVotes(count) {
    return count + (count === 1 ? " vote" : " votes");
}

function formatDate(isoDate) {
    return new Date(isoDate).toLocaleDateString();
}
