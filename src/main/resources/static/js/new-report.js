const reportForm = document.getElementById("report-form");
const message = document.getElementById("message");

reportForm.addEventListener("submit", async event => {
    event.preventDefault();
    showMessage(message, "Sending...");

    const report = Object.fromEntries(new FormData(reportForm));
    report.category = report.category || null;

    try {
        const createdReport = await createReport(report);
        window.location.href = "report.html?id=" + createdReport.id;
    } catch (error) {
        showMessage(message, error.message, "error");
    }
});
