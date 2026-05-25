async function loadPatientData() {
    try {
        // Backend API endpoint eka call 
        const response = await fetch('http://localhost:8080/api/patients/info');
        const data = await response.json();

        // UI update 
        document.getElementById('p-id').innerText = data.id;
        document.getElementById('p-name').innerText = data.name;
        document.getElementById('p-history').innerText = data.medicalHistory;

        // Info box 
        document.getElementById('patient-info').style.display = 'block';
    } catch (error) {
        console.error("Error fetching data:", error);
        alert("Backend එකට connect වෙන්න බැරි වුණා!");
    }
}
