from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import requests
from bs4 import BeautifulSoup
import uvicorn
import logging
import os

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

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.get("/api/blood-stock")
def get_blood_stock():
    url = "https://eraktkosh.mohfw.gov.in/BLDAHIMS/bloodbank/stockAvailability.cnt?hmode=GETBLOODSTOCKDETAILS&stateCode=-1&districtCode=-1&bloodGroup=all&bloodComponent=-1"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')
        
        table = soup.find('table', id='example-table')
        if not table:
            return {"status": "error", "message": "Table not found. Source format may have changed."}
        
        tbody = table.find('tbody')
        if not tbody:
            return {"status": "error", "message": "Table body not found. Source format may have changed."}
            
        rows = tbody.find_all('tr')
        data = []
        for row in rows:
            cols = row.find_all('td')
            if len(cols) >= 4:
                # The eRaktKosh table columns:
                # S.No, Blood Bank, Category, Availability, Last Updated
                bank_name = cols[1].get_text(separator=" ", strip=True)
                availability = cols[3].get_text(separator=" ", strip=True)
                # Ensure bank name exists
                if bank_name:
                    data.append({
                        "bank_name": bank_name,
                        "availability": availability
                    })
        
        return {"status": "success", "data": data}
        
    except requests.RequestException as e:
        logger.error(f"Error fetching data: {e}")
        return {"status": "error", "message": "Failed to fetch data from source"}
    except Exception as e:
        logger.error(f"Unexpected error: {e}")
        return {"status": "error", "message": "An unexpected error occurred during parsing"}

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
