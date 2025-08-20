import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export interface Contract {
  id: string;
  title: string;
  content: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  status: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface ContractAnalysisResult {
  contractId: string;
  risks: Array<{
    clause: string;
    reason: string;
    riskLevel: string;
  }>;
  summary: string;
  recommendations: string[];
}

class ContractService {
  private getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };
  }

  async getAllContracts(): Promise<Contract[]> {
    const response = await axios.get(`${API_BASE_URL}/contracts`, {
      headers: this.getAuthHeaders()
    });
    return response.data;
  }

  async getContract(id: string): Promise<Contract> {
    const response = await axios.get(`${API_BASE_URL}/contracts/${id}`, {
      headers: this.getAuthHeaders()
    });
    return response.data;
  }

  async uploadContract(file: File): Promise<Contract> {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await axios.post(`${API_BASE_URL}/contracts/upload`, formData, {
      headers: {
        ...this.getAuthHeaders(),
        'Content-Type': 'multipart/form-data'
      }
    });
    return response.data;
  }

  async analyzeContract(contractId: string): Promise<ContractAnalysisResult> {
    const response = await axios.post(
      `${API_BASE_URL}/contracts/${contractId}/analyze`,
      {},
      { headers: this.getAuthHeaders() }
    );
    return response.data;
  }

  async getDashboardStats(): Promise<any> {
    const response = await axios.get(`${API_BASE_URL}/contracts/stats`, {
      headers: this.getAuthHeaders()
    });
    return response.data;
  }

  async deleteContract(id: string): Promise<void> {
    await axios.delete(`${API_BASE_URL}/contracts/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  async updateContract(id: string, data: Partial<Contract>): Promise<Contract> {
    const response = await axios.put(
      `${API_BASE_URL}/contracts/${id}`,
      data,
      { headers: this.getAuthHeaders() }
    );
    return response.data;
  }
}

export const contractService = new ContractService();