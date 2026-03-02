import sys
import importlib.util

def check_install(package):
    if importlib.util.find_spec(package) is None:
        print(f"{package} not found. Please install it using: pip install {package}")
        return False
    return True

try:
    import pypdf
except ImportError:
    print("Error: pypdf module not found.")
    sys.exit(1)

def extract_text(pdf_path):
    try:
        reader = pypdf.PdfReader(pdf_path)
        print(f"Update: Successfully opened PDF. Total pages: {len(reader.pages)}")
        
        full_text = []
        for i, page in enumerate(reader.pages):
            text = page.extract_text()
            if text:
                full_text.append(f"--- Page {i+1} ---\n{text}")
        
        return "\n".join(full_text)
    except Exception as e:
        return f"Error reading PDF: {str(e)}"

if __name__ == "__main__":
    pdf_path = r"c:\Users\Administrator\Desktop\论文修改\20251655_稿件全文（doc_docx）O (1).pdf"
    content = extract_text(pdf_path)
    # Output specifically formatted for the agent to read
    print("<<<PAPER_CONTENT_START>>>")
    print(content)
    print("<<<PAPER_CONTENT_END>>>")
