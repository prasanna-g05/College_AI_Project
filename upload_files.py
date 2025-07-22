import os
import requests

def upload_files():
    upload_url = "http://localhost:8000/upload"
    files_to_upload = []
    
    # Find all PDF files in uploaded_pdfs
    for filename in os.listdir("uploaded_pdfs"):
        if filename.endswith('.pdf'):
            file_path = os.path.join("uploaded_pdfs", filename)
            files_to_upload.append(('files', (filename, open(file_path, 'rb'), 'application/pdf')))
    
    print(f"Uploading {len(files_to_upload)} files...")
    
    try:
        response = requests.post(upload_url, files=files_to_upload)
        print(f"Upload response: {response.status_code}")
        print(f"Response body: {response.text}")
        
        if response.status_code == 200:
            print("✅ Files uploaded successfully!")
        else:
            print("❌ Upload failed!")
            
    except Exception as e:
        print(f"Upload error: {e}")
    finally:
        # Close file handles
        for _, (_, file_handle, _) in files_to_upload:
            file_handle.close()

if __name__ == "__main__":
    upload_files()
