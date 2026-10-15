const STATUSES = ["REPORTED", "IN_PROGRESS", "FIXED", "REJECTED"];

const message = document.getElementById("message");
const reportList = document.getElementById("report-list");

function createStatusSelect(report, badge) {
    const select = document.createElement("select");
    select.setAttribute("aria-label", "Status of " + report.title);

    for (const status of STATUSES) {
        const option = document.createElement("option");
        option.value = status;
        option.textContent = formatLabel(status);
        option.selected = status === report.status;
        select.append(option);
    }

    select.addEventListener("change", () => changeStatus(report, select, badge));
    return select;
}

function createCell(content) {
    const cell = document.createElement("td");
    cell.append(content);
    return cell;
}

function createReportRow(report) {
    const link = document.createElement("a");
    link.href = "report.html?id=" + report.id;
    link.textContent = report.title;

    const badge = document.createElement("span");
    badge.className = badgeClass(report.status);
    badge.textContent = formatLabel(report.status);

    const row = document.createElement("tr");
    row.append(
        createCell(link),
        createCell(report.area),
        createCell(formatLabel(report.category)),
        createCell(badge),
        createCell(formatVotes(report.upvoteCount)),
        createCell(createStatusSelect(report, badge))
    );
    return row;
}

async function changeStatus(report, select, badge) {
    const newStatus = select.value;
    showMessage(message, "Saving...");

    try {
        await updateStatus(report.id, newStatus);
        report.status = newStatus;
        badge.className = badgeClass(newStatus);
        badge.textContent = formatLabel(newStatus);
        showMessage(
            message,
            "Status of \"" + report.title + "\" changed to " + formatLabel(newStatus) + ".",
            "success"
        );
    } catch (error) {
        showMessage(message, "Could not change the status: " + error.message, "error");
        select.value = report.status;
    }
}

async function loadReports() {
    showMessage(message, "Loading reports...");

    try {
        const reports = await getReports({});
        reportList.replaceChildren(...reports.map(createReportRow));
        showMessage(message, "");
    } catch (error) {
        showMessage(message, "Could not load reports: " + error.message, "error");
    }
}

loadReports();
