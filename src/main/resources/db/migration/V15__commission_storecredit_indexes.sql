CREATE INDEX idx_seller_commission_salesperson ON seller_commission(salesperson_id);
CREATE INDEX idx_seller_commission_sale_date ON seller_commission(sale_date DESC);
CREATE INDEX idx_seller_commission_status ON seller_commission(status);

CREATE INDEX idx_store_credit_note_customer ON store_credit_note(customer_id);
CREATE INDEX idx_store_credit_note_status ON store_credit_note(status);
