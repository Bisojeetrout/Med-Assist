from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
import requests
import uvicorn
import logging
import os
import re

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

STATES = [
    {"value": "35", "label": "Andaman and Nicobar Islands"},
    {"value": "28", "label": "Andhra Pradesh"},
    {"value": "12", "label": "Arunachal Pradesh"},
    {"value": "18", "label": "Assam"},
    {"value": "10", "label": "Bihar"},
    {"value": "94", "label": "Chandigarh"},
    {"value": "22", "label": "Chhattisgarh"},
    {"value": "25", "label": "Dadra And Nagar Haveli And Daman And Diu"},
    {"value": "97", "label": "Delhi"},
    {"value": "30", "label": "Goa"},
    {"value": "24", "label": "Gujarat"},
    {"value": "96", "label": "Haryana"},
    {"value": "92", "label": "Himachal Pradesh"},
    {"value": "91", "label": "Jammu and Kashmir"},
    {"value": "20", "label": "Jharkhand"},
    {"value": "29", "label": "Karnataka"},
    {"value": "32", "label": "Kerala"},
    {"value": "37", "label": "Ladakh"},
    {"value": "31", "label": "Lakshadweep"},
    {"value": "23", "label": "Madhya Pradesh"},
    {"value": "27", "label": "Maharashtra"},
    {"value": "14", "label": "Manipur"},
    {"value": "17", "label": "Meghalaya"},
    {"value": "15", "label": "Mizoram"},
    {"value": "13", "label": "Nagaland"},
    {"value": "21", "label": "Odisha"},
    {"value": "34", "label": "Puducherry"},
    {"value": "93", "label": "Punjab"},
    {"value": "98", "label": "Rajasthan"},
    {"value": "11", "label": "Sikkim"},
    {"value": "33", "label": "Tamil Nadu"},
    {"value": "36", "label": "Telangana"},
    {"value": "16", "label": "Tripura"},
    {"value": "95", "label": "Uttarakhand"},
    {"value": "99", "label": "Uttar Pradesh"},
    {"value": "19", "label": "West Bengal"}
]

def clean_html(raw_html: str) -> str:
    """Removes HTML tags and replaces <br> with spaces."""
    if not isinstance(raw_html, str):
        return str(raw_html)
    # Replace <br> or <br/> with space to separate lines
    text = re.sub(r'<br\s*/?>', ' ', raw_html)
    # Remove all other tags
    clean_text = re.sub(r'<.*?>', '', text)
    return clean_text.strip()

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.get("/api/states")
def get_states():
    """Returns a list of all states available in e-RaktKosh."""
    return {"status": "success", "data": STATES}

@app.get("/api/districts")
def get_districts(state_code: str = Query(..., description="The state code to fetch districts for")):
    """Proxies the e-RaktKosh district list API."""
    url = f"https://eraktkosh.mohfw.gov.in/BLDAHIMS/bloodbank/nearbyBB.cnt?hmode=GETDISTRICTLIST&selectedStateCode={state_code}"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()
        if "records" in data:
            # Map {"id": "Name", "value": "123"} to our output format
            districts = [{"value": d["value"], "label": d["id"]} for d in data["records"] if d["value"] != "-1"]
            return {"status": "success", "data": districts}
        return {"status": "error", "message": "Invalid response format from source"}
    except Exception as e:
        logger.error(f"Error fetching districts: {e}")
        return {"status": "error", "message": "Failed to fetch districts"}

@app.get("/api/blood-stock")
def get_blood_stock(
    state_code: str = Query(..., description="State code"),
    district_code: str = Query("-1", description="District code (-1 for all)"),
    blood_group: str = Query("all", description="Blood group code"),
    blood_component: str = Query("11", description="Blood component code (11=Whole Blood)")
):
    # e-RaktKosh AJAX JSON API
    url = f"https://eraktkosh.mohfw.gov.in/BLDAHIMS/bloodbank/nearbyBB.cnt?hmode=GETNEARBYSTOCKDETAILS&stateCode={state_code}&districtCode={district_code}&bloodGroup={blood_group}&bloodComponent={blood_component}&lang=0"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=15)
        response.raise_for_status()
        json_data = response.json()
        
        if "data" not in json_data:
            return {"status": "error", "message": "Invalid data format received from source."}
            
        parsed_data = []
        for row in json_data["data"]:
            if len(row) >= 5:
                # e-RaktKosh JSON format:
                # 0: S.No
                # 1: Blood Bank (HTML formatted)
                # 2: Category (Govt. / Private)
                # 3: Availability (HTML formatted)
                # 4: Last Updated
                bank_name_raw = row[1]
                availability_raw = row[3]
                last_updated = row[4]
                
                # Clean HTML
                bank_name = clean_html(bank_name_raw)
                availability = clean_html(availability_raw)
                
                if bank_name:
                    parsed_data.append({
                        "bank_name": bank_name,
                        "availability": availability,
                        "last_updated": last_updated,
                        "category": row[2] if row[2] != "null" else "Unknown"
                    })
                    
        return {"status": "success", "data": parsed_data}
        
    except requests.RequestException as e:
        logger.error(f"Error fetching data: {e}")
        return {"status": "error", "message": "Failed to fetch data from source"}
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return {"status": "error", "message": "An unexpected error occurred during parsing"}

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port)
