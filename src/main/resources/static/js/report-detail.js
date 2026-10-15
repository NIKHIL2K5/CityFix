const reportId = new URLSearchParams(window.location.search).get("id");
const message = document.getElementById("message");
const reportDetails = document.getElementById("report");
const upvoteForm = document.getElementById("upvote-form");
const upvoteMessage = document.getElementById("upvote-message");

function showReport(report) {
    document.getElementById("title").textContent = report.title;
    document.getElementById("status").textContent = formatLabel(report.status);
    document.getElementById("status").className = badgeClass(report.status);
    document.getElementById("votes").textContent = formatVotes(report.upvoteCount);
    document.getElementById("description").textContent = report.description;
    document.getElementById("category").textContent = formatLabel(report.category);
    document.getElementById("area").textContent = report.area;
    document.getElementById("address").textContent = report.address || "Not given";
    document.getElementById("reporter").textContent = report.reporterName;
    document.getElementById("created").textContent = formatDate(report.createdAt);
    document.getElementById("fixed").textContent = report.fixedAt ? formatDate(report.fixedAt) : "Not fixed yet";
}

async function loadReport() {
    if (!reportId) {
        showMessage(message, "No report selected.", "error");
        return;
    }

    try {
        const report = await getReport(reportId);
        showReport(report);
        showMessage(message, "");
        reportDetails.hidden = false;
        upvoteForm.hidden = false;
    } catch (error) {
        showMessage(message, "Could not load the report: " + error.message, "error");
    }
}

upvoteForm.addEventListener("submit", async event => {
    event.preventDefault();
    showMessage(upvoteMessage, "Sending...");

    try {
        const report = await upvoteReport(reportId, document.getElementById("voterEmail").value);
        showReport(report);
        upvoteForm.reset();
        showMessage(upvoteMessage, "Thank you, your vote was added.", "success");
    } catch (error) {
        showMessage(upvoteMessage, error.message, "error");
    }
});

loadReport();
