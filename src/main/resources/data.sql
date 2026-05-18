INSERT INTO subscription_plans (
    name, price, color_hex, icon_key, features, 
    sms_alerte, nombre_business, cout, nombre_jour_souscription, 
    dashboard, email_alerte, loyalty_access, is_active, 
    nombre_categorie_par_business, nombre_prod_par_business,priorite
) 
VALUES 
('BASE', 'Gratuit', '#FF9800', 'star_border', 'Inscription 1 business,2 categories,Mode hors internet intégré,5 produits par categorie', 
 false, 1, 0, 30, false, false, false, true, 2, 5,1),

('PRO', '5 000 FCFA / mois', '#FF5722', 'star_half', 'Inscription 2 business,Categories illimitées,Mode hors internet intégré,Support prioritaire', 
 true, 2, 5000, 30, true, true, true, true, 999, 999,2),

('PREMIUM', '10 000 FCFA / mois', '#F44336', 'star', 'Inscription illimitée,Fonctionnalités base,Dashbord ventes/depenses,Alertes stock SMS,Support VIP', 
 true, 999, 10000, 30, true, true, true, true, 999, 999,3)

ON CONFLICT (name) DO NOTHING;