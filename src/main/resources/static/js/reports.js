const filterForm = document.getElementById("filter-form");
const statsText = document.getElementById("stats");
const message = document.getElementById("message");
const reportList = document.getElementById("report-list");

function createReportItem(report) {
    const status = document.createElement("span");
    status.className = badgeClass(report.status);
    status.textContent = formatLabel(report.status);

    const category = document.createElement("span");
    category.className = "tag";
    category.textContent = formatLabel(report.category);

    const badges = document.createElement("div");
    badges.className = "card-badges";
    badges.append(status, category);

    const link = document.createElement("a");
    link.href = "report.html?id=" + report.id;
    link.textContent = report.title;

    const title = document.createElement("h2");
    title.className = "card-title";
    title.append(link);

    const place = document.createElement("p");
    place.className = "card-place";
    place.textContent = report.address ? report.area + " - " + report.address : report.area;

    const votes = document.createElement("p");
    votes.className = "votes";
    votes.textContent = formatVotes(report.upvoteCount);

    const item = document.createElement("li");
    item.className = "card report-card";
    item.append(badges, title, place, votes);
    return item;
}

function showStats(stats) {
    const parts = Object.entries(stats.byStatus)
        .map(([status, count]) => formatLabel(status) + ": " + count);
    statsText.textContent = parts.join(" | ");
}

async function loadReports() {
    showMessage(message, "Loading reports...");
    reportList.replaceChildren();

    try {
        const filters = Object.fromEntries(new FormData(filterForm));
        const [reports, stats] = await Promise.all([getReports(filters), getStats()]);
        showStats(stats);
        showMessage(
            message,
            reports.length === 0
                ? "No reports found. Try changing the filters."
                : ""
            );
        reports.forEach(report => reportList.append(createReportItem(report)));
    } catch (error) {
        showMessage(message, "Could not load reports: " + error.message, "error");
    }
}

filterForm.addEventListener("submit", event => {
    event.preventDefault();
    loadReports();
});

loadReports();
