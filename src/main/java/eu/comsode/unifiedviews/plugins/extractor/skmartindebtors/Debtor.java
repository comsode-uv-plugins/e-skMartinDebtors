package eu.comsode.unifiedviews.plugins.extractor.skmartindebtors;

import java.util.ArrayList;
import java.util.List;

public class Debtor {

    private String debtorId;

    private String name;

    private String address;

    private String city;

    private Double debtsSum;

    private String currency;

    private List<DebtDetail> details;

    Debtor() {
        details = new ArrayList<DebtDetail>();
    }

    public String getDebtorId() {
        return debtorId;
    }

    public void setDebtorId(String debtorId) {
        this.debtorId = debtorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getDebtsSum() {
        return debtsSum;
    }

    public void setDebtsSum(Double debtsSum) {
        this.debtsSum = debtsSum;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<DebtDetail> getDetails() {
        return details;
    }

    public void setDetails(List<DebtDetail> details) {
        this.details = details;
    }

    /*****************************************************************/
    public void addDetail(DebtDetail dd) {
        details.add(dd);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dlznik: ").append(name);
        sb.append(", Adresa: ").append(address);
        sb.append(", Mesto: ").append(city);
        sb.append(", Celkovy dlh: ").append(debtsSum);
        sb.append(", Mena: ").append(currency);
        sb.append(", [");
        for (DebtDetail dd : details) {
            sb.append("Typ dlhu: ").append(dd.getDebtType());
            sb.append(", Dlzna suma: ").append(dd.getDebtSumForType());
            sb.append(", Mena: ").append(dd.getCurrency());
            sb.append(", Variabilny symbol: ").append(dd.getVariableSymbol());
            sb.append(", Specificky symbol: ").append(dd.getSpecificSymbol());
        }
        sb.append("]");
        return sb.toString();
    }
}

class DebtDetail {
    private String debtType;

    private Double debtSumForType;

    private String currency;

    private String variableSymbol;

    private String specificSymbol;

    public String getDebtType() {
        return debtType;
    }

    public void setDebtType(String debtType) {
        this.debtType = debtType;
    }

    public Double getDebtSumForType() {
        return debtSumForType;
    }

    public void setDebtSumForType(Double debtSumForType) {
        this.debtSumForType = debtSumForType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public void setVariableSymbol(String variableSymbol) {
        this.variableSymbol = variableSymbol;
    }

    public String getSpecificSymbol() {
        return specificSymbol;
    }

    public void setSpecificSymbol(String specificSymbol) {
        this.specificSymbol = specificSymbol;
    }

}
